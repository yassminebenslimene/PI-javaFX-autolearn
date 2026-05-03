"""
AutoLearn Face ID Server - Python handles camera, Java displays frames via HTTP.
"""

import os
import sys
import json
import base64
import shutil
import threading
import time
from pathlib import Path

# Désactiver TensorFlow pour économiser la mémoire
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "3"
os.environ["TF_ENABLE_ONEDNN_OPTS"] = "0"
os.environ["CUDA_VISIBLE_DEVICES"] = "-1"  # Pas de GPU

import cv2
import numpy as np
from flask import Flask, request, jsonify, Response
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

STORAGE_DIR = Path.home() / ".autolearn" / "faces"
STORAGE_DIR.mkdir(parents=True, exist_ok=True)

# Global camera state
camera_lock = threading.Lock()
current_cap = None
streaming = False
face_cascade = None
deepface_loaded = False
_persistent_cap = None
_persistent_cap_lock = threading.Lock()

def get_cascade():
    global face_cascade
    if face_cascade is None:
        cascade_path = cv2.data.haarcascades + 'haarcascade_frontalface_default.xml'
        face_cascade = cv2.CascadeClassifier(cascade_path)
    return face_cascade

def load_deepface():
    # Désactivé pour économiser la mémoire - utilise OpenCV uniquement
    return False

def open_camera():
    """Open camera - tries DirectShow first (most stable on Windows), then others."""
    os.environ["OPENCV_VIDEOIO_MSMF_ENABLE_HW_TRANSFORMS"] = "0"

    # DirectShow first — avoids MSMF initStream failures on Windows
    backends = [
        (cv2.CAP_DSHOW, "DirectShow"),
        (cv2.CAP_ANY,   "Any"),
        (cv2.CAP_MSMF,  "MSMF"),
    ]

    for backend, name in backends:
        for idx in [0, 1, 2]:
            try:
                cap = cv2.VideoCapture(idx, backend)
                if cap.isOpened():
                    # Warm-up: read a few frames
                    for _ in range(3):
                        cap.read()
                    ret, frame = cap.read()
                    if ret and frame is not None:
                        cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
                        cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
                        print(f"[Camera] Opened index={idx} backend={name}")
                        return cap
                cap.release()
            except Exception as e:
                print(f"[Camera] {name} idx={idx} failed: {e}")

    return None


def get_persistent_camera():
    """Returns a shared camera instance, opening it once and reusing it."""
    global _persistent_cap
    with _persistent_cap_lock:
        if _persistent_cap is not None and _persistent_cap.isOpened():
            return _persistent_cap
        print("[Camera] Opening persistent camera...")
        _persistent_cap = open_camera()
        return _persistent_cap


def release_persistent_camera():
    global _persistent_cap
    with _persistent_cap_lock:
        if _persistent_cap is not None:
            _persistent_cap.release()
            _persistent_cap = None
            print("[Camera] Persistent camera released.")

def get_user_dir(user_id):
    d = STORAGE_DIR / f"user_{user_id}"
    d.mkdir(parents=True, exist_ok=True)
    return d

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

@app.route('/frame', methods=['GET'])
def get_frame():
    """
    Returns a single JPEG frame from the persistent camera with face detection overlay.
    Java calls this every 100ms — camera stays open between calls.
    """
    cap = get_persistent_camera()
    if cap is None:
        return jsonify({"error": "no camera"}), 500

    with _persistent_cap_lock:
        ret, frame = cap.read()
        if not ret or frame is None:
            # Camera may have gone stale — reset it
            cap.release()
            global _persistent_cap
            _persistent_cap = None
            return jsonify({"error": "no frame"}), 500

    # Draw face detection overlay
    det = get_cascade()
    if det and not det.empty():
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        faces = det.detectMultiScale(gray, 1.1, 4, minSize=(60, 60))

        h, w = frame.shape[:2]
        cx, cy, rx, ry = w // 2, h // 2, 120, 150

        detected = False
        for (x, y, fw, fh) in faces:
            fcx, fcy = x + fw // 2, y + fh // 2
            dx, dy = (fcx - cx) / rx, (fcy - cy) / ry
            if dx * dx + dy * dy <= 1.0:
                detected = True
                cv2.rectangle(frame, (x, y), (x + fw, y + fh), (0, 220, 100), 2)

        color = (0, 220, 100) if detected else (200, 200, 200)
        cv2.ellipse(frame, (cx, cy), (rx, ry), 0, 0, 360, color, 2)

        msg = "Parfait ! Restez immobile" if detected else "Centrez votre visage dans le cercle"
        cv2.rectangle(frame, (0, h - 30), (w, h), (0, 0, 0), -1)
        cv2.putText(frame, msg, (10, h - 8), cv2.FONT_HERSHEY_SIMPLEX, 0.6, color, 1)

    # Encode as JPEG base64
    _, buf = cv2.imencode('.jpg', frame, [cv2.IMWRITE_JPEG_QUALITY, 80])
    b64 = base64.b64encode(buf.tobytes()).decode('utf-8')

    # Detect face for status
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    faces = det.detectMultiScale(gray, 1.1, 4, minSize=(60, 60)) if det and not det.empty() else []
    h, w = frame.shape[:2]
    cx, cy, rx, ry = w // 2, h // 2, 120, 150
    face_in_circle = False
    for (x, y, fw, fh) in faces:
        fcx, fcy = x + fw // 2, y + fh // 2
        dx, dy = (fcx - cx) / rx, (fcy - cy) / ry
        if dx * dx + dy * dy <= 1.0:
            face_in_circle = True
            break

    return jsonify({"frame": b64, "face_detected": face_in_circle})

@app.route('/register', methods=['POST'])
def register():
    user_id = request.args.get('userId', '')
    if not user_id:
        return jsonify({"success": False, "message": "userId manquant"}), 400

    det = get_cascade()
    cap = get_persistent_camera()
    if cap is None:
        return jsonify({"success": False, "message": "Webcam non disponible"}), 500

    user_dir = get_user_dir(user_id)
    for f in user_dir.glob("*.jpg"):
        f.unlink()

    captured = 0
    target = 15
    start_time = time.time()

    print(f"[FaceID] Registering user {user_id}")

    try:
        while captured < target and (time.time() - start_time) < 40:
            with _persistent_cap_lock:
                ret, frame = cap.read()
            if not ret or frame is None:
                time.sleep(0.05)
                continue

            if det and not det.empty():
                gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
                faces = det.detectMultiScale(gray, 1.1, 3, minSize=(60, 60))
                if len(faces) > 0:
                    x, y, w, h = max(faces, key=lambda f: f[2] * f[3])
                    face_img = cv2.resize(frame[y:y+h, x:x+w], (160, 160))
                    cv2.imwrite(str(user_dir / f"face_{captured}.jpg"), face_img)
                    captured += 1
                    print(f"[FaceID] Captured {captured}/{target}")
                    time.sleep(0.35)
            else:
                cv2.imwrite(str(user_dir / f"face_{captured}.jpg"), cv2.resize(frame, (160, 160)))
                captured += 1
                time.sleep(0.35)
    except Exception as e:
        print(f"[FaceID] Register error: {e}")

    if captured == 0:
        return jsonify({"success": False, "message": "Aucun visage detecte. Assurez-vous d etre bien eclaire."}), 500

    return jsonify({"success": True, "message": f"Visage enregistre ! ({captured} photos)", "captured": captured})

@app.route('/authenticate', methods=['POST'])
def authenticate():
    user_id = request.args.get('userId', '')
    user_dir = STORAGE_DIR / f"user_{user_id}"
    if not user_dir.exists() or not any(user_dir.glob("*.jpg")):
        return jsonify({"success": False, "message": "Aucun visage enregistre"}), 404

    det = get_cascade()
    cap = get_persistent_camera()
    if cap is None:
        return jsonify({"success": False, "message": "Webcam non disponible"}), 500

    live_face = None
    start_time = time.time()

    try:
        while live_face is None and (time.time() - start_time) < 15:
            with _persistent_cap_lock:
                ret, frame = cap.read()
            if not ret or frame is None:
                time.sleep(0.05)
                continue
            if det and not det.empty():
                gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
                faces = det.detectMultiScale(gray, 1.1, 3, minSize=(60, 60))
                if len(faces) > 0:
                    x, y, w, h = max(faces, key=lambda f: f[2] * f[3])
                    live_face = cv2.resize(frame[y:y+h, x:x+w], (160, 160))
            else:
                live_face = cv2.resize(frame, (160, 160))
    except Exception as e:
        print(f"[FaceID] Auth error: {e}")

    if live_face is None:
        return jsonify({"success": False, "message": "Aucun visage detecte. Regardez la camera."}), 400

    # Compare with stored faces
    stored_files = list(user_dir.glob("*.jpg"))
    live_gray = cv2.cvtColor(live_face, cv2.COLOR_BGR2GRAY)
    live_hist = cv2.calcHist([live_gray], [0], None, [64], [0, 256])
    cv2.normalize(live_hist, live_hist, 0, 1, cv2.NORM_MINMAX)

    best_score = 0
    for sf in stored_files:
        stored = cv2.imread(str(sf), cv2.IMREAD_GRAYSCALE)
        if stored is None: continue
        sh = cv2.calcHist([stored], [0], None, [64], [0, 256])
        cv2.normalize(sh, sh, 0, 1, cv2.NORM_MINMAX)
        score = cv2.compareHist(live_hist, sh, cv2.HISTCMP_CORREL)
        if score > best_score: best_score = score

    print(f"[FaceID] Auth score: {best_score:.3f}")

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
    return jsonify({"success": True})


@app.route('/release_camera', methods=['POST'])
def release_camera():
    release_persistent_camera()
    return jsonify({"success": True})

if __name__ == '__main__':
    print("=" * 50)
    print("AutoLearn Face ID Server - Port 5001")
    print("=" * 50)
    threading.Thread(target=load_deepface, daemon=True).start()
    app.run(host='127.0.0.1', port=5001, debug=False, threaded=True)
