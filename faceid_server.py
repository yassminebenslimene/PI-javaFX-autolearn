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

    # Load face detector
    cascade_path = cv2.data.haarcascades + 'haarcascade_frontalface_default.xml'
    face_cascade = cv2.CascadeClassifier(cascade_path)
    if face_cascade.empty():
        return jsonify({"success": False, "message": "Detecteur non disponible"}), 500

    # Open camera
    cap = open_camera()
    if cap is None:
        return jsonify({"success": False, "message": "Webcam non disponible"}), 500

    user_dir = get_user_dir(user_id)
    # Clear old data
    for f in user_dir.glob("*.jpg"):
        f.unlink()

    captured = 0
    target = 20
    last_capture = 0

    print(f"[FaceID] Starting registration for user {user_id}")

    try:
        while captured < target:
            ret, frame = cap.read()
            if not ret or frame is None:
                time.sleep(0.05)
                continue

            now = time.time()
            if now - last_capture < 0.25:  # max 4fps
                time.sleep(0.05)
                continue
            last_capture = now

            gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
            faces = face_cascade.detectMultiScale(gray, 1.05, 4, minSize=(60, 60))

            if len(faces) > 0:
                x, y, w, h = max(faces, key=lambda f: f[2] * f[3])
                margin = int(w * 0.1)
                x1, y1 = max(0, x - margin), max(0, y - margin)
                x2, y2 = min(frame.shape[1], x + w + margin), min(frame.shape[0], y + h + margin)
                face_img = frame[y1:y2, x1:x2]
                face_resized = cv2.resize(face_img, (160, 160))

                path = str(user_dir / f"face_{captured}.jpg")
                cv2.imwrite(path, face_resized)
                captured += 1
                print(f"[FaceID] Captured {captured}/{target}")

    finally:
        cap.release()

    if captured < target // 2:
        return jsonify({"success": False, "message": f"Seulement {captured} photos capturees. Reessayez avec un meilleur eclairage."}), 500

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

    # Load face detector
    cascade_path = cv2.data.haarcascades + 'haarcascade_frontalface_default.xml'
    face_cascade = cv2.CascadeClassifier(cascade_path)

    # Open camera
    cap = open_camera()
    if cap is None:
        return jsonify({"success": False, "message": "Webcam non disponible"}), 500

    try:
        # Capture live face
        live_face, _ = capture_face_frame(cap, face_cascade)
    finally:
        cap.release()

    if live_face is None:
        return jsonify({"success": False, "message": "Aucun visage detecte. Regardez la camera."}), 400

    # Save temp file for comparison
    temp_path = str(STORAGE_DIR / f"temp_{user_id}.jpg")
    live_resized = cv2.resize(live_face, (160, 160))
    cv2.imwrite(temp_path, live_resized)

    # Compare with stored faces
    if load_deepface():
        try:
            from deepface import DeepFace
            stored_files = list(user_dir.glob("*.jpg"))
            best_score = 0
            matches = 0

            for stored_file in stored_files[:10]:  # compare with first 10
                try:
                    result = DeepFace.verify(
                        img1_path=temp_path,
                        img2_path=str(stored_file),
                        model_name="ArcFace",
                        enforce_detection=False,
                        silent=True
                    )
                    if result["verified"]:
                        matches += 1
                    distance = result.get("distance", 1.0)
                    similarity = max(0, 1.0 - distance)
                    if similarity > best_score:
                        best_score = similarity
                except Exception as e:
                    print(f"[FaceID] Compare error: {e}")
                    continue

            os.remove(temp_path)

            threshold = 0.4  # ArcFace threshold
            verified = best_score >= threshold or matches >= 2

            print(f"[FaceID] Best score: {best_score:.3f}, matches: {matches}, verified: {verified}")

            if verified:
                return jsonify({
                    "success": True,
                    "message": f"Identite verifiee ! ({int(best_score * 100)}% de correspondance)",
                    "confidence": best_score
                })
            else:
                return jsonify({
                    "success": False,
                    "message": f"Visage non reconnu ({int(best_score * 100)}%). Reessayez.",
                    "confidence": best_score
                })

        except Exception as e:
            print(f"[FaceID] DeepFace error: {e}")
            # Fallback to histogram comparison
            pass

    # Fallback: histogram comparison (no DeepFace)
    os.remove(temp_path)
    return histogram_authenticate(user_id, live_resized)

def histogram_authenticate(user_id, live_face):
    """Fallback authentication using histogram comparison."""
    user_dir = STORAGE_DIR / f"user_{user_id}"
    stored_files = list(user_dir.glob("*.jpg"))

    live_gray = cv2.cvtColor(live_face, cv2.COLOR_BGR2GRAY)
    live_hist = cv2.calcHist([live_gray], [0], None, [64], [0, 256])
    cv2.normalize(live_hist, live_hist, 0, 1, cv2.NORM_MINMAX)

    best_score = 0
    for stored_file in stored_files:
        stored = cv2.imread(str(stored_file), cv2.IMREAD_GRAYSCALE)
        if stored is None:
            continue
        stored_hist = cv2.calcHist([stored], [0], None, [64], [0, 256])
        cv2.normalize(stored_hist, stored_hist, 0, 1, cv2.NORM_MINMAX)
        score = cv2.compareHist(live_hist, stored_hist, cv2.HISTCMP_CORREL)
        if score > best_score:
            best_score = score

    threshold = 0.55
    if best_score >= threshold:
        return jsonify({
            "success": True,
            "message": f"Identite verifiee ! ({int(best_score * 100)}%)",
            "confidence": best_score
        })
    else:
        return jsonify({
            "success": False,
            "message": f"Visage non reconnu ({int(best_score * 100)}%). Reessayez.",
            "confidence": best_score
        })

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
