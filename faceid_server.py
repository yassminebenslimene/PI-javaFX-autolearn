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
current_cap = None          # persistent camera handle
camera_last_used = 0        # timestamp of last use
CAMERA_IDLE_TIMEOUT = 10    # seconds before auto-releasing idle camera
streaming = False
face_cascade = None
deepface_loaded = False

def get_cascade():
    global face_cascade
    if face_cascade is None:
        cascade_path = cv2.data.haarcascades + 'haarcascade_frontalface_default.xml'
        face_cascade = cv2.CascadeClassifier(cascade_path)
    return face_cascade

def load_deepface():
    global deepface_loaded
    if not deepface_loaded:
        try:
            from deepface import DeepFace
            deepface_loaded = True
            print("[FaceID] DeepFace ready!")
        except ImportError:
            deepface_loaded = False
    return deepface_loaded

def _create_camera():
    """Create a new camera capture, trying multiple indices and backends."""
    backends = [cv2.CAP_DSHOW, cv2.CAP_MSMF, 0]  # DirectShow, Media Foundation, default
    indices = [0, 1, 2]

    for idx in indices:
        for backend in backends:
            try:
                cap = cv2.VideoCapture(idx, backend) if backend != 0 else cv2.VideoCapture(idx)
                if cap.isOpened():
                    cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
                    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
                    cap.set(cv2.CAP_PROP_FPS, 30)
                    # Warm up: discard first few frames
                    for _ in range(3):
                        cap.read()
                    ret, test = cap.read()
                    if ret and test is not None:
                        print(f"[FaceID] Camera opened: index={idx}, backend={backend}")
                        return cap
                cap.release()
            except Exception as e:
                print(f"[FaceID] Camera idx={idx} backend={backend} failed: {e}")
    return None

def open_camera():
    """Return the persistent camera handle, opening it if needed."""
    global current_cap, camera_last_used
    with camera_lock:
        camera_last_used = time.time()
        if current_cap is not None and current_cap.isOpened():
            return current_cap
        # (Re)open
        current_cap = _create_camera()
        return current_cap

def release_camera():
    """Explicitly release the camera (called after register/authenticate)."""
    global current_cap
    with camera_lock:
        if current_cap is not None:
            current_cap.release()
            current_cap = None
        print("[FaceID] Camera released.")

def _camera_idle_watchdog():
    """Background thread: release camera if idle for CAMERA_IDLE_TIMEOUT seconds."""
    global current_cap, camera_last_used
    while True:
        time.sleep(5)
        with camera_lock:
            if current_cap is not None and current_cap.isOpened():
                if time.time() - camera_last_used > CAMERA_IDLE_TIMEOUT:
                    current_cap.release()
                    current_cap = None
                    print("[FaceID] Camera auto-released (idle).")

def get_user_dir(user_id):
    d = STORAGE_DIR / f"user_{user_id}"
    d.mkdir(parents=True, exist_ok=True)
    return d

# ── Face comparison helpers ───────────────────────────────────────────────────

def _cache_embeddings(user_id, user_dir):
    """Pre-compute DeepFace embeddings for all registered face images and save to disk."""
    if not deepface_loaded:
        return
    try:
        from deepface import DeepFace
        embeddings = []
        for img_path in sorted(user_dir.glob("*.jpg")):
            try:
                result = DeepFace.represent(
                    img_path=str(img_path),
                    model_name="Facenet512",
                    enforce_detection=False,
                    detector_backend="opencv"
                )
                if result:
                    embeddings.append(result[0]["embedding"])
            except Exception as e:
                print(f"[FaceID] Embedding failed for {img_path.name}: {e}")

        if embeddings:
            avg = np.mean(embeddings, axis=0).tolist()
            cache_path = user_dir / "embeddings.json"
            with open(cache_path, "w") as f:
                json.dump({"embeddings": embeddings, "average": avg}, f)
            print(f"[FaceID] Cached {len(embeddings)} embeddings for user {user_id}")
    except Exception as e:
        print(f"[FaceID] Cache embeddings error: {e}")

def _cosine_similarity(a, b):
    a, b = np.array(a), np.array(b)
    denom = np.linalg.norm(a) * np.linalg.norm(b)
    return float(np.dot(a, b) / denom) if denom > 0 else 0.0

def _deepface_verify(user_id, user_dir, live_face_bgr):
    """
    Compare live face against stored embeddings using Facenet512.
    Returns a result dict with success/message/confidence.
    Threshold: cosine similarity >= 0.68 (strict enough to reject family members).
    """
    from deepface import DeepFace

    THRESHOLD = 0.68   # tune: higher = stricter. 0.68 rejects most family members.

    # Get live embedding
    tmp_path = str(STORAGE_DIR / f"_live_{user_id}.jpg")
    cv2.imwrite(tmp_path, live_face_bgr)
    try:
        live_result = DeepFace.represent(
            img_path=tmp_path,
            model_name="Facenet512",
            enforce_detection=False,
            detector_backend="opencv"
        )
    finally:
        try: os.remove(tmp_path)
        except: pass

    if not live_result:
        return {"success": False, "message": "Impossible d'analyser le visage.", "confidence": 0}

    live_emb = live_result[0]["embedding"]

    # Load cached embeddings if available
    cache_path = user_dir / "embeddings.json"
    stored_embeddings = []
    if cache_path.exists():
        with open(cache_path) as f:
            data = json.load(f)
            stored_embeddings = data.get("embeddings", [])

    # Fallback: compute on-the-fly from images
    if not stored_embeddings:
        for img_path in sorted(user_dir.glob("*.jpg")):
            try:
                r = DeepFace.represent(
                    img_path=str(img_path),
                    model_name="Facenet512",
                    enforce_detection=False,
                    detector_backend="opencv"
                )
                if r:
                    stored_embeddings.append(r[0]["embedding"])
            except Exception:
                pass

    if not stored_embeddings:
        return {"success": False, "message": "Donnees biometriques corrompues. Re-enregistrez.", "confidence": 0}

    # Score = best cosine similarity across all stored embeddings
    best_score = max(_cosine_similarity(live_emb, s) for s in stored_embeddings)
    pct = int(best_score * 100)
    print(f"[FaceID] DeepFace cosine similarity: {best_score:.4f} (threshold={THRESHOLD})")

    if best_score >= THRESHOLD:
        return {"success": True,  "message": f"Identite verifiee ! ({pct}%)", "confidence": best_score}
    else:
        return {"success": False, "message": f"Visage non reconnu ({pct}%). Reessayez.", "confidence": best_score}

def _lbp_verify(user_dir, live_face_bgr):
    """
    Fallback when DeepFace is unavailable.
    Uses LBP (Local Binary Patterns) — much better than plain histogram.
    """
    def lbp_hist(img_gray):
        h, w = img_gray.shape
        lbp = np.zeros_like(img_gray, dtype=np.uint8)
        for i in range(1, h - 1):
            for j in range(1, w - 1):
                center = img_gray[i, j]
                code = 0
                neighbors = [
                    img_gray[i-1, j-1], img_gray[i-1, j], img_gray[i-1, j+1],
                    img_gray[i,   j+1], img_gray[i+1, j+1], img_gray[i+1, j],
                    img_gray[i+1, j-1], img_gray[i,   j-1]
                ]
                for k, n in enumerate(neighbors):
                    if n >= center:
                        code |= (1 << k)
                lbp[i, j] = code
        hist = cv2.calcHist([lbp], [0], None, [256], [0, 256])
        cv2.normalize(hist, hist, 0, 1, cv2.NORM_MINMAX)
        return hist

    live_gray = cv2.cvtColor(cv2.resize(live_face_bgr, (64, 64)), cv2.COLOR_BGR2GRAY)
    live_hist = lbp_hist(live_gray)

    best_score = 0.0
    for sf in user_dir.glob("*.jpg"):
        stored = cv2.imread(str(sf), cv2.IMREAD_GRAYSCALE)
        if stored is None: continue
        stored = cv2.resize(stored, (64, 64))
        sh = lbp_hist(stored)
        score = cv2.compareHist(live_hist, sh, cv2.HISTCMP_CORREL)
        if score > best_score:
            best_score = score

    pct = int(best_score * 100)
    print(f"[FaceID] LBP fallback score: {best_score:.4f}")
    THRESHOLD = 0.82   # LBP needs a higher threshold
    if best_score >= THRESHOLD:
        return {"success": True,  "message": f"Identite verifiee ! ({pct}%)", "confidence": best_score}
    else:
        return {"success": False, "message": f"Visage non reconnu ({pct}%). Reessayez.", "confidence": best_score}

# ── Routes ────────────────────────────────────────────────────────────────────

@app.route('/status', methods=['GET'])
def status():
    return jsonify({"status": "running", "deepface": deepface_loaded})

@app.route('/camera_status', methods=['GET'])
def camera_status():
    """Diagnostic: try to open camera and report result."""
    cap = _create_camera()
    if cap is None:
        return jsonify({"camera": False, "message": "Cannot open camera - check if another app is using it"}), 500
    cap.release()
    return jsonify({"camera": True, "message": "Camera accessible"})

@app.route('/has_face', methods=['GET'])
def has_face():
    user_id = request.args.get('userId', '')
    user_dir = STORAGE_DIR / f"user_{user_id}"
    has = user_dir.exists() and any(user_dir.glob("*.jpg"))
    return jsonify({"has_face": has})

@app.route('/frame', methods=['GET'])
def get_frame():
    """
    Returns a single JPEG frame from the camera with face detection overlay.
    Java calls this every 100ms to display live feed.
    Uses a persistent camera handle to avoid the open/close overhead.
    """
    cap = open_camera()
    if cap is None:
        return jsonify({"error": "no camera"}), 500

    ret, frame = cap.read()
    if not ret or frame is None:
        # Try once more before giving up
        ret, frame = cap.read()
    if frame is None:
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
    cap = open_camera()
    if cap is None:
        return jsonify({"success": False, "message": "Webcam non disponible"}), 500

    user_dir = get_user_dir(user_id)
    for f in user_dir.glob("*.jpg"):
        f.unlink()

    captured = 0
    target = 20          # more samples → better embedding average
    last_capture = 0
    start_time = time.time()

    print(f"[FaceID] Registering user {user_id}")

    try:
        while captured < target and (time.time() - start_time) < 45:
            ret, frame = cap.read()
            if not ret or frame is None:
                time.sleep(0.05)
                continue

            now = time.time()
            if now - last_capture < 0.4:   # enforce 400 ms between captures
                time.sleep(0.05)
                continue
            last_capture = now

            if det and not det.empty():
                gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
                faces = det.detectMultiScale(gray, 1.1, 3, minSize=(80, 80))
                if len(faces) > 0:
                    x, y, w, h = max(faces, key=lambda f: f[2] * f[3])
                    # Save the full-colour crop at 224×224 (needed by DeepFace)
                    face_img = cv2.resize(frame[y:y+h, x:x+w], (224, 224))
                    cv2.imwrite(str(user_dir / f"face_{captured}.jpg"), face_img)
                    captured += 1
                    print(f"[FaceID] Captured {captured}/{target}")
            else:
                cv2.imwrite(str(user_dir / f"face_{captured}.jpg"), cv2.resize(frame, (224, 224)))
                captured += 1
    finally:
        release_camera()

    if captured == 0:
        return jsonify({"success": False, "message": "Aucun visage detecte. Assurez-vous d etre bien eclaire."}), 500

    # Pre-compute and cache embeddings so first login is fast
    _cache_embeddings(user_id, user_dir)

    return jsonify({"success": True, "message": f"Visage enregistre ! ({captured} photos)", "captured": captured})

@app.route('/authenticate', methods=['POST'])
def authenticate():
    user_id = request.args.get('userId', '')
    user_dir = STORAGE_DIR / f"user_{user_id}"
    if not user_dir.exists() or not any(user_dir.glob("*.jpg")):
        return jsonify({"success": False, "message": "Aucun visage enregistre"}), 404

    det = get_cascade()
    cap = open_camera()
    if cap is None:
        return jsonify({"success": False, "message": "Webcam non disponible"}), 500

    live_face = None
    live_frame = None
    start_time = time.time()

    try:
        while live_face is None and (time.time() - start_time) < 15:
            ret, frame = cap.read()
            if not ret or frame is None:
                time.sleep(0.05)
                continue
            if det and not det.empty():
                gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
                faces = det.detectMultiScale(gray, 1.1, 3, minSize=(80, 80))
                if len(faces) > 0:
                    x, y, w, h = max(faces, key=lambda f: f[2] * f[3])
                    live_face = cv2.resize(frame[y:y+h, x:x+w], (224, 224))
                    live_frame = frame
            else:
                live_face = cv2.resize(frame, (224, 224))
                live_frame = frame
    finally:
        release_camera()

    if live_face is None:
        return jsonify({"success": False, "message": "Aucun visage detecte. Regardez la camera."}), 400

    # ── DeepFace embedding comparison (accurate) ──────────────────────────────
    if deepface_loaded:
        try:
            result = _deepface_verify(user_id, user_dir, live_face)
            print(f"[FaceID] DeepFace result: {result}")
            return jsonify(result)
        except Exception as e:
            print(f"[FaceID] DeepFace error, falling back: {e}")

    # ── Fallback: LBP histogram (better than plain histogram) ─────────────────
    return jsonify(_lbp_verify(user_dir, live_face))

@app.route('/delete', methods=['DELETE'])
def delete():
    user_id = request.args.get('userId', '')
    user_dir = STORAGE_DIR / f"user_{user_id}"
    if user_dir.exists():
        shutil.rmtree(user_dir)
    return jsonify({"success": True})

@app.route('/stop_camera', methods=['POST'])
def stop_camera():
    """Explicitly release the camera (call when Face ID dialog closes)."""
    release_camera()
    return jsonify({"success": True})

if __name__ == '__main__':
    print("=" * 50)
    print("AutoLearn Face ID Server - Port 5001")
    print("=" * 50)
    threading.Thread(target=load_deepface, daemon=True).start()
    threading.Thread(target=_camera_idle_watchdog, daemon=True).start()
    app.run(host='127.0.0.1', port=5001, debug=False, threaded=True)
