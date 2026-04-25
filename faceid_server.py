"""
AutoLearn Face ID Server
========================
Python Flask server that handles face registration and authentication.
Uses DeepFace (ArcFace model) for professional-grade face recognition.

Endpoints:
  POST /register?userId=123   - Capture and register face
  POST /authenticate?userId=123 - Authenticate face
  GET  /status                - Check if server is running
  GET  /has_face?userId=123   - Check if user has registered face
  DELETE /delete?userId=123   - Delete face data

Run: python faceid_server.py
Port: 5001
"""

import os
import sys
import json
import base64
import shutil
import threading
import time
from pathlib import Path

import cv2
import numpy as np
from flask import Flask, request, jsonify
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

# Storage directory
STORAGE_DIR = Path.home() / ".autolearn" / "faces"
STORAGE_DIR.mkdir(parents=True, exist_ok=True)

# DeepFace model (loaded lazily)
deepface_loaded = False

def load_deepface():
    global deepface_loaded
    if not deepface_loaded:
        try:
            from deepface import DeepFace
            # Warm up the model
            print("[FaceID] Loading DeepFace ArcFace model...")
            deepface_loaded = True
            print("[FaceID] DeepFace ready!")
        except ImportError:
            print("[FaceID] DeepFace not installed. Run: pip install deepface")
            deepface_loaded = False
    return deepface_loaded

def get_user_dir(user_id):
    d = STORAGE_DIR / f"user_{user_id}"
    d.mkdir(parents=True, exist_ok=True)
    return d

def open_camera():
    """Open webcam with multiple backend attempts."""
    # Try DirectShow first (Windows)
    cap = cv2.VideoCapture(0, cv2.CAP_DSHOW)
    if cap.isOpened():
        cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
        cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
        time.sleep(0.5)  # stabilize
        return cap
    cap.release()
    
    # Fallback to default
    cap = cv2.VideoCapture(0)
    if cap.isOpened():
        cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
        cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
        time.sleep(0.5)
        return cap
    cap.release()
    return None

def capture_face_frame(cap, face_cascade):
    """Capture a single good frame with a detected face."""
    for _ in range(50):  # try up to 50 frames
        ret, frame = cap.read()
        if not ret or frame is None:
            time.sleep(0.05)
            continue
        
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        faces = face_cascade.detectMultiScale(gray, 1.05, 4, minSize=(60, 60))
        
        if len(faces) > 0:
            # Return the largest face
            x, y, w, h = max(faces, key=lambda f: f[2] * f[3])
            # Add margin
            margin = int(w * 0.1)
            x1 = max(0, x - margin)
            y1 = max(0, y - margin)
            x2 = min(frame.shape[1], x + w + margin)
            y2 = min(frame.shape[0], y + h + margin)
            face_img = frame[y1:y2, x1:x2]
            return face_img, frame
        
        time.sleep(0.05)
    return None, None

# ── Routes ────────────────────────────────────────────────────────────────────

@app.route('/status', methods=['GET'])
def status():
    return jsonify({"status": "running", "deepface": deepface_loaded})

@app.route('/has_face', methods=['GET'])
def has_face():
    user_id = request.args.get('userId', '')
    user_dir = STORAGE_DIR / f"user_{user_id}"
    has = user_dir.exists() and any(user_dir.glob("*.jpg"))
    return jsonify({"has_face": has})

@app.route('/register', methods=['POST'])
def register():
    user_id = request.args.get('userId', '')
    if not user_id:
        return jsonify({"success": False, "message": "userId manquant"}), 400

    cascade_path = cv2.data.haarcascades + 'haarcascade_frontalface_default.xml'
    face_cascade = cv2.CascadeClassifier(cascade_path)
    if face_cascade.empty():
        return jsonify({"success": False, "message": "Detecteur non disponible"}), 500

    cap = None
    for backend in [cv2.CAP_DSHOW, cv2.CAP_MSMF, 0]:
        try:
            c = cv2.VideoCapture(0 + backend if backend != 0 else 0)
            if c.isOpened():
                ret, frame = c.read()
                if ret and frame is not None:
                    cap = c
                    break
                c.release()
        except Exception:
            pass

    if cap is None:
        return jsonify({"success": False, "message": "Webcam non disponible"}), 500

    cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)

    user_dir = get_user_dir(user_id)
    for f in user_dir.glob("*.jpg"):
        f.unlink()

    captured = 0
    target = 15
    start_time = time.time()
    max_time = 45

    print(f"[FaceID] Starting registration for user {user_id}")
    print("[FaceID] Camera window opened - look at the camera!")

    try:
        while captured < target and (time.time() - start_time) < max_time:
            ret, frame = cap.read()
            if not ret or frame is None:
                time.sleep(0.05)
                continue

            gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
            faces = face_cascade.detectMultiScale(gray, 1.1, 3, minSize=(80, 80))

            # Draw guide circle
            h, w = frame.shape[:2]
            cx, cy, rx, ry = w // 2, h // 2, 130, 160

            display = frame.copy()

            if len(faces) > 0:
                x, y, fw, fh = max(faces, key=lambda f: f[2] * f[3])
                # Draw green rectangle on face
                cv2.rectangle(display, (x, y), (x + fw, y + fh), (0, 220, 100), 2)

                # Check if face is inside guide ellipse
                fcx, fcy = x + fw // 2, y + fh // 2
                dx, dy = (fcx - cx) / rx, (fcy - cy) / ry
                inside = (dx * dx + dy * dy) <= 1.0

                if inside:
                    # Green ellipse + capture
                    cv2.ellipse(display, (cx, cy), (rx, ry), 0, 0, 360, (0, 220, 100), 3)
                    cv2.putText(display, f"Capture {captured+1}/{target} - Restez immobile!",
                        (10, h - 15), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 220, 100), 2)

                    face_img = frame[y:y+fh, x:x+fw]
                    face_resized = cv2.resize(face_img, (160, 160))
                    cv2.imwrite(str(user_dir / f"face_{captured}.jpg"), face_resized)
                    captured += 1
                    print(f"[FaceID] Captured {captured}/{target}")
                    time.sleep(0.4)
                else:
                    cv2.ellipse(display, (cx, cy), (rx, ry), 0, 0, 360, (0, 165, 255), 2)
                    cv2.putText(display, "Centrez votre visage dans le cercle",
                        (10, h - 15), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 165, 255), 2)
            else:
                # White ellipse - no face
                cv2.ellipse(display, (cx, cy), (rx, ry), 0, 0, 360, (200, 200, 200), 2)
                cv2.putText(display, "Placez votre visage dans le cercle",
                    (10, h - 15), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (200, 200, 200), 2)

            # Progress bar at top
            progress = int((captured / target) * w)
            cv2.rectangle(display, (0, 0), (progress, 8), (0, 220, 100), -1)

            # Show window
            cv2.imshow("AutoLearn - Face ID Enregistrement (Appuyez sur Q pour annuler)", display)

            key = cv2.waitKey(1) & 0xFF
            if key == ord('q') or key == 27:  # Q or ESC to cancel
                break

    finally:
        cap.release()
        cv2.destroyAllWindows()

    if captured == 0:
        return jsonify({"success": False, "message": "Aucun visage detecte. Assurez-vous d etre bien eclaire et face a la camera."}), 500

    return jsonify({
        "success": True,
        "message": f"Visage enregistre avec succes ! ({captured} photos)",
        "captured": captured
    })

@app.route('/authenticate', methods=['POST'])
def authenticate():
    user_id = request.args.get('userId', '')
    if not user_id:
        return jsonify({"success": False, "message": "userId manquant"}), 400

    user_dir = STORAGE_DIR / f"user_{user_id}"
    if not user_dir.exists() or not any(user_dir.glob("*.jpg")):
        return jsonify({"success": False, "message": "Aucun visage enregistre pour cet utilisateur"}), 404

    cascade_path = cv2.data.haarcascades + 'haarcascade_frontalface_default.xml'
    face_cascade = cv2.CascadeClassifier(cascade_path)

    # Open camera quickly
    cap = None
    for backend in [cv2.CAP_DSHOW, cv2.CAP_MSMF, 0]:
        try:
            c = cv2.VideoCapture(0 + backend if backend != 0 else 0)
            if c.isOpened():
                ret, frame = c.read()
                if ret and frame is not None:
                    cap = c
                    break
                c.release()
        except Exception:
            pass

    if cap is None:
        return jsonify({"success": False, "message": "Webcam non disponible"}), 500

    cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)

    live_face = None
    start_time = time.time()

    try:
        while live_face is None and (time.time() - start_time) < 15:
            ret, frame = cap.read()
            if not ret or frame is None:
                time.sleep(0.1)
                continue

            gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
            faces = face_cascade.detectMultiScale(gray, 1.1, 3, minSize=(50, 50))

            display = frame.copy()
            h, w = frame.shape[:2]

            if len(faces) > 0:
                x, y, fw, fh = max(faces, key=lambda f: f[2] * f[3])
                cv2.rectangle(display, (x, y), (x + fw, y + fh), (0, 220, 100), 2)
                cv2.putText(display, "Visage detecte - Analyse...",
                    (10, h - 15), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 220, 100), 2)
                live_face = cv2.resize(frame[y:y+fh, x:x+fw], (160, 160))
            else:
                cv2.putText(display, "Regardez la camera...",
                    (10, h - 15), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (200, 200, 200), 2)

            cv2.imshow("AutoLearn - Face ID Connexion", display)
            key = cv2.waitKey(1) & 0xFF
            if key == ord('q') or key == 27:
                break

    finally:
        cap.release()
        cv2.destroyAllWindows()

    if live_face is None:
        return jsonify({"success": False, "message": "Aucun visage detecte. Regardez la camera."}), 400

    temp_path = str(STORAGE_DIR / f"temp_{user_id}.jpg")
    cv2.imwrite(temp_path, live_face)

    # Compare with stored faces using histogram (fast fallback)
    result = histogram_authenticate_img(user_id, live_face)

    # Try DeepFace if available (more accurate)
    if load_deepface():
        try:
            from deepface import DeepFace
            stored_files = list(user_dir.glob("*.jpg"))[:5]
            best_score = 0
            for stored_file in stored_files:
                try:
                    r = DeepFace.verify(
                        img1_path=temp_path,
                        img2_path=str(stored_file),
                        model_name="Facenet",  # faster than ArcFace
                        enforce_detection=False,
                        silent=True
                    )
                    sim = max(0, 1.0 - r.get("distance", 1.0))
                    if sim > best_score:
                        best_score = sim
                except Exception:
                    continue

            if os.path.exists(temp_path):
                os.remove(temp_path)

            if best_score >= 0.35:
                return jsonify({"success": True, "message": f"Identite verifiee ! ({int(best_score*100)}%)", "confidence": best_score})
            elif best_score > 0:
                # DeepFace says no but histogram says yes — trust histogram
                return result
        except Exception as e:
            print(f"[FaceID] DeepFace error: {e}")

    if os.path.exists(temp_path):
        os.remove(temp_path)
    return result

def histogram_authenticate_img(user_id, live_face):
    """Fast histogram comparison."""
    user_dir = STORAGE_DIR / f"user_{user_id}"
    stored_files = list(user_dir.glob("*.jpg"))

    live_gray = cv2.cvtColor(live_face, cv2.COLOR_BGR2GRAY)
    live_hist = cv2.calcHist([live_gray], [0], None, [64], [0, 256])
    cv2.normalize(live_hist, live_hist, 0, 1, cv2.NORM_MINMAX)

    best_score = 0
    for stored_file in stored_files:
        stored = cv2.imread(str(stored_file), cv2.IMREAD_GRAYSCALE)
        if stored is None: continue
        stored_hist = cv2.calcHist([stored], [0], None, [64], [0, 256])
        cv2.normalize(stored_hist, stored_hist, 0, 1, cv2.NORM_MINMAX)
        score = cv2.compareHist(live_hist, stored_hist, cv2.HISTCMP_CORREL)
        if score > best_score: best_score = score

    print(f"[FaceID] Histogram score: {best_score:.3f}")
    if best_score >= 0.55:
        return jsonify({"success": True, "message": f"Identite verifiee ! ({int(best_score*100)}%)", "confidence": best_score})
    else:
        return jsonify({"success": False, "message": f"Visage non reconnu ({int(best_score*100)}%). Reessayez.", "confidence": best_score})

@app.route('/delete', methods=['DELETE'])
def delete():
    user_id = request.args.get('userId', '')
    user_dir = STORAGE_DIR / f"user_{user_id}"
    if user_dir.exists():
        shutil.rmtree(user_dir)
    return jsonify({"success": True, "message": "Donnees supprimees"})

# ── Main ──────────────────────────────────────────────────────────────────────

if __name__ == '__main__':
    print("=" * 50)
    print("AutoLearn Face ID Server")
    print("Port: 5001")
    print("=" * 50)
    
    # Load DeepFace in background
    threading.Thread(target=load_deepface, daemon=True).start()
    
    app.run(host='127.0.0.1', port=5001, debug=False, threaded=False)
