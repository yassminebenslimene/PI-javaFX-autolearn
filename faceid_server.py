"""
AutoLearn Face ID Server - Python handles camera, Java displays frames via HTTP.
Singleton camera — une seule instance partagée entre tous les threads.
"""

import os
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

STORAGE_DIR = Path.home() / ".autolearn" / "faces"
STORAGE_DIR.mkdir(parents=True, exist_ok=True)

# ── Singleton caméra ──────────────────────────────────────────────────────────
_cam_lock   = threading.Lock()
_cam        = None          # VideoCapture singleton
_cam_index  = -1
face_cascade = None
deepface_loaded = False

def _open_cam_once():
    """Ouvre la caméra une seule fois et la garde ouverte."""
    global _cam, _cam_index
    for idx in range(4):
        cap = cv2.VideoCapture(idx)
        if cap.isOpened():
            ret, frame = cap.read()
            if ret and frame is not None:
                cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
                cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
                cap.set(cv2.CAP_PROP_FPS, 30)
                _cam = cap
                _cam_index = idx
                print(f"[FaceID] Camera ouverte sur index {idx}")
                return True
        cap.release()
    print("[FaceID] ERREUR: aucune camera disponible")
    return False

def get_cam():
    """Retourne le singleton caméra, l'ouvre si nécessaire."""
    global _cam
    with _cam_lock:
        if _cam is not None and _cam.isOpened():
            return _cam
        _open_cam_once()
        return _cam

def read_frame_safe():
    """Lit un frame de façon thread-safe."""
    with _cam_lock:
        if _cam is None or not _cam.isOpened():
            return None
        ret, frame = _cam.read()
        if ret and frame is not None:
            return frame
        # Tentative de réouverture
        _cam.release()
        if _open_cam_once():
            ret, frame = _cam.read()
            return frame if ret else None
        return None

# ── Cascade Haar ──────────────────────────────────────────────────────────────

def get_cascade():
    global face_cascade
    if face_cascade is None:
        path = cv2.data.haarcascades + 'haarcascade_frontalface_default.xml'
        face_cascade = cv2.CascadeClassifier(path)
    return face_cascade

def load_deepface():
    global deepface_loaded
    try:
        from deepface import DeepFace
        deepface_loaded = True
        print("[FaceID] DeepFace ready!")
    except ImportError:
        deepface_loaded = False

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
    """Retourne un frame JPEG encodé en base64 avec overlay détection visage."""
    frame = read_frame_safe()
    if frame is None:
        return jsonify({"error": "no camera"}), 500

    det = get_cascade()
    face_in_circle = False

    if det and not det.empty():
        gray  = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        faces = det.detectMultiScale(gray, 1.1, 4, minSize=(60, 60))
        h, w  = frame.shape[:2]
        cx, cy, rx, ry = w // 2, h // 2, 120, 150

        for (x, y, fw, fh) in faces:
            fcx, fcy = x + fw // 2, y + fh // 2
            if ((fcx - cx) / rx) ** 2 + ((fcy - cy) / ry) ** 2 <= 1.0:
                face_in_circle = True
                cv2.rectangle(frame, (x, y), (x + fw, y + fh), (0, 220, 100), 2)

        color = (0, 220, 100) if face_in_circle else (200, 200, 200)
        cv2.ellipse(frame, (cx, cy), (rx, ry), 0, 0, 360, color, 2)
        msg = "Parfait ! Restez immobile" if face_in_circle else "Centrez votre visage dans le cercle"
        cv2.rectangle(frame, (0, h - 30), (w, h), (0, 0, 0), -1)
        cv2.putText(frame, msg, (10, h - 8), cv2.FONT_HERSHEY_SIMPLEX, 0.6, color, 1)

    _, buf = cv2.imencode('.jpg', frame, [cv2.IMWRITE_JPEG_QUALITY, 80])
    b64 = base64.b64encode(buf.tobytes()).decode('utf-8')
    return jsonify({"frame": b64, "face_detected": face_in_circle})

@app.route('/register', methods=['POST'])
def register():
    user_id = request.args.get('userId', '')
    if not user_id:
        return jsonify({"success": False, "message": "userId manquant"}), 400

    # S'assurer que la caméra est ouverte
    if get_cam() is None:
        return jsonify({"success": False, "message": "Webcam non disponible"}), 500

    det = get_cascade()
    user_dir = get_user_dir(user_id)
    for f in user_dir.glob("*.jpg"):
        f.unlink()

    captured = 0
    target   = 15
    start    = time.time()
    print(f"[FaceID] Registering user {user_id}")

    while captured < target and (time.time() - start) < 40:
        frame = read_frame_safe()
        if frame is None:
            time.sleep(0.05)
            continue
        if det and not det.empty():
            gray  = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
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

    if captured == 0:
        return jsonify({"success": False, "message": "Aucun visage detecte. Assurez-vous d etre bien eclaire."}), 500
    return jsonify({"success": True, "message": f"Visage enregistre ! ({captured} photos)", "captured": captured})

@app.route('/authenticate', methods=['POST'])
def authenticate():
    user_id  = request.args.get('userId', '')
    user_dir = STORAGE_DIR / f"user_{user_id}"
    if not user_dir.exists() or not any(user_dir.glob("*.jpg")):
        return jsonify({"success": False, "message": "Aucun visage enregistre"}), 404

    if get_cam() is None:
        return jsonify({"success": False, "message": "Webcam non disponible"}), 500

    det = get_cascade()
    live_face = None
    start = time.time()

    while live_face is None and (time.time() - start) < 15:
        frame = read_frame_safe()
        if frame is None:
            time.sleep(0.05)
            continue
        if det and not det.empty():
            gray  = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
            faces = det.detectMultiScale(gray, 1.1, 3, minSize=(60, 60))
            if len(faces) > 0:
                x, y, w, h = max(faces, key=lambda f: f[2] * f[3])
                live_face = cv2.resize(frame[y:y+h, x:x+w], (160, 160))
        else:
            live_face = cv2.resize(frame, (160, 160))

    if live_face is None:
        return jsonify({"success": False, "message": "Aucun visage detecte. Regardez la camera."}), 400

    stored_files = list(user_dir.glob("*.jpg"))
    live_gray = cv2.cvtColor(live_face, cv2.COLOR_BGR2GRAY)
    live_hist = cv2.calcHist([live_gray], [0], None, [64], [0, 256])
    cv2.normalize(live_hist, live_hist, 0, 1, cv2.NORM_MINMAX)

    best_score = 0.0
    for sf in stored_files:
        stored = cv2.imread(str(sf), cv2.IMREAD_GRAYSCALE)
        if stored is None:
            continue
        sh = cv2.calcHist([stored], [0], None, [64], [0, 256])
        cv2.normalize(sh, sh, 0, 1, cv2.NORM_MINMAX)
        score = cv2.compareHist(live_hist, sh, cv2.HISTCMP_CORREL)
        if score > best_score:
            best_score = score

    print(f"[FaceID] Auth score: {best_score:.3f}")

    if best_score >= 0.55:
        return jsonify({"success": True,  "message": f"Identite verifiee ! ({int(best_score*100)}%)", "confidence": best_score})
    else:
        return jsonify({"success": False, "message": f"Visage non reconnu ({int(best_score*100)}%). Reessayez.", "confidence": best_score})

@app.route('/delete', methods=['DELETE'])
def delete():
    user_id  = request.args.get('userId', '')
    user_dir = STORAGE_DIR / f"user_{user_id}"
    if user_dir.exists():
        shutil.rmtree(user_dir)
    return jsonify({"success": True})

if __name__ == '__main__':
    print("=" * 50)
    print("AutoLearn Face ID Server - Port 5001")
    print("=" * 50)
    # Ouvrir la caméra au démarrage (avant les requêtes)
    _open_cam_once()
    threading.Thread(target=load_deepface, daemon=True).start()
    app.run(host='127.0.0.1', port=5001, debug=False, threaded=True)
