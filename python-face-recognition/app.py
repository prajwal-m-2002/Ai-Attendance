from fastapi import FastAPI, UploadFile, File, Form
import numpy as np
import cv2
import json

app = FastAPI()

face_cascade = cv2.CascadeClassifier(
    cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
)

@app.get("/")
def root():
    return {"message": "Face API is running (OpenCV only, no dlib)."}

@app.post("/encode-face")
async def encode_face(file: UploadFile = File(...)):
    image_bytes = await file.read()
    np_img = np.frombuffer(image_bytes, np.uint8)
    img = cv2.imdecode(np_img, cv2.IMREAD_COLOR)
    if img is None:
        return {"success": False, "message": "Could not decode image"}

    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    faces = face_cascade.detectMultiScale(gray, 1.1, 5, minSize=(60, 60))
    if len(faces) == 0:
        return {"success": False, "message": "No face detected"}

    x, y, w, h = max(faces, key=lambda f: f[2] * f[3])
    face_roi = gray[y:y+h, x:x+w]
    face_resized = cv2.resize(face_roi, (100, 100))
    encoding = face_resized.flatten().astype(float) / 255.0

    return {
        "success": True,
        "width": 100,
        "height": 100,
        "vector_length": len(encoding),
        "encoding": encoding.tolist()
    }

# 🔹 NEW: recognize-face
@app.post("/recognize-face")
async def recognize_face(
        file: UploadFile = File(...),
        known_encodings_json: str = Form(...)
):
    """
    known_encodings_json: JSON list of encoding vectors [[...], [...], ...]
    """
    try:
        known_encodings = json.loads(known_encodings_json)
        known_encodings = [np.array(k, dtype=float) for k in known_encodings]
    except Exception as e:
        return {"success": False, "message": f"Bad encodings JSON: {e}"}

    # encode incoming face (same as above)
    image_bytes = await file.read()
    np_img = np.frombuffer(image_bytes, np.uint8)
    img = cv2.imdecode(np_img, cv2.IMREAD_COLOR)
    if img is None:
        return {"success": False, "message": "Could not decode image"}

    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    faces = face_cascade.detectMultiScale(gray, 1.1, 5, minSize=(60, 60))
    if len(faces) == 0:
        return {"success": False, "message": "No face detected"}

    x, y, w, h = max(faces, key=lambda f: f[2] * f[3])
    face_roi = gray[y:y+h, x:x+w]
    face_resized = cv2.resize(face_roi, (100, 100))
    unknown = face_resized.flatten().astype(float) / 255.0

    # compare with each known encoding (Euclidean distance)
    distances = [np.linalg.norm(unknown - k) for k in known_encodings]
    best_index = int(np.argmin(distances))
    best_distance = float(distances[best_index])

    return {
        "success": True,
        "best_index": best_index,
        "distance": best_distance
    }
