
from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
import numpy as np
import cv2
import json
import logging
import os

# =========================
# Logging configuration
# =========================
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# =========================
# Matching configuration
# =========================
# You can tweak this via env var on Render: MATCH_THRESHOLD=25
MATCH_THRESHOLD = float(os.getenv("MATCH_THRESHOLD", "25.0"))
# Distance at which confidence ~ 0%
CONFIDENCE_MAX_DISTANCE = float(os.getenv("CONFIDENCE_MAX_DISTANCE", "40.0"))

logger.info(f"Using MATCH_THRESHOLD={MATCH_THRESHOLD}, "
            f"CONFIDENCE_MAX_DISTANCE={CONFIDENCE_MAX_DISTANCE}")

# =========================
# FastAPI app
# =========================
app = FastAPI(
    title="Face Recognition API",
    description="Face encoding and recognition service using OpenCV",
    version="1.1.0"
)

# CORS (open for now – restrict in production)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # In production, specify your Spring Boot URL
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# =========================
# Load face cascade classifier
# =========================
try:
    face_cascade = cv2.CascadeClassifier(
        cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
    )
    if face_cascade.empty():
        raise Exception("Failed to load face cascade classifier")
    logger.info("Face cascade classifier loaded successfully")
except Exception as e:
    logger.error(f"Error loading face cascade: {e}")
    raise


# =========================
# Health / root endpoints
# =========================
@app.api_route("/", methods=["GET", "HEAD"])
def root():
    """Health check endpoint (GET + HEAD)."""
    return {
        "message": "Face Recognition API is running",
        "status": "healthy",
        "engine": "OpenCV"
    }


@app.get("/health")
def health_check():
    """Detailed health check."""
    return {
        "status": "healthy",
        "cascade_loaded": not face_cascade.empty(),
        "service": "face-recognition-api"
    }


# =========================
# /encode-face
# =========================
@app.post("/encode-face")
async def encode_face(file: UploadFile = File(...)):
    """
    Encode a face from an uploaded image.
    Returns a flattened vector representation of the face.
    """
    try:
        # Read and decode image
        image_bytes = await file.read()
        np_img = np.frombuffer(image_bytes, np.uint8)
        img = cv2.imdecode(np_img, cv2.IMREAD_COLOR)

        if img is None:
            logger.error("Could not decode image")
            return {
                "success": False,
                "message": "Could not decode image. Please upload a valid image file."
            }

        # Convert to grayscale
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

        # Detect faces
        faces = face_cascade.detectMultiScale(
            gray,
            scaleFactor=1.1,
            minNeighbors=5,
            minSize=(60, 60)
        )

        if len(faces) == 0:
            logger.warning("No face detected in image")
            return {
                "success": False,
                "message": "No face detected. Please ensure your face is clearly visible in the image."
            }

        # Get the largest face
        x, y, w, h = max(faces, key=lambda f: f[2] * f[3])
        logger.info(f"Face detected at position: x={x}, y={y}, w={w}, h={h}")

        # Extract and resize face
        face_roi = gray[y:y + h, x:x + w]
        face_resized = cv2.resize(face_roi, (100, 100))

        # Create encoding (normalized flattened array)
        encoding = face_resized.flatten().astype(float) / 255.0

        logger.info(f"Face encoded successfully. Vector length: {len(encoding)}")

        return {
            "success": True,
            "message": "Face encoded successfully",
            "width": 100,
            "height": 100,
            "vector_length": len(encoding),
            "encoding": encoding.tolist(),
            "faces_detected": len(faces)
        }

    except Exception as e:
        logger.error(f"Error encoding face: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Error processing image: {str(e)}")


# =========================
# /recognize-face
# =========================
@app.post("/recognize-face")
async def recognize_face(
    file: UploadFile = File(...),
    known_encodings_json: str = Form(...)
):
    """
    Recognize a face by comparing it with known encodings.

    Parameters:
    - file: Image file containing a face
    - known_encodings_json: JSON array of known face encodings [[...], [...], ...]

    Returns:
    - success: bool
    - best_index: index of best-matching encoding
    - distance: Euclidean distance to best match
    - is_match: True/False based on threshold
    - threshold: current threshold used
    - confidence: rough confidence percentage
    """
    try:
        # ---- Parse known encodings ----
        try:
            known_encodings_raw = json.loads(known_encodings_json)
            if not isinstance(known_encodings_raw, list) or len(known_encodings_raw) == 0:
                return {
                    "success": False,
                    "message": "known_encodings_json must be a non-empty array"
                }

            known_encodings = [np.array(k, dtype=float) for k in known_encodings_raw]
            logger.info(f"Loaded {len(known_encodings)} known encodings")
        except json.JSONDecodeError as e:
            logger.error(f"Invalid JSON: {e}")
            return {
                "success": False,
                "message": f"Invalid JSON format: {str(e)}"
            }
        except Exception as e:
            logger.error(f"Error parsing encodings: {e}")
            return {
                "success": False,
                "message": f"Error parsing encodings: {str(e)}"
            }

        # ---- Read and decode image ----
        image_bytes = await file.read()
        np_img = np.frombuffer(image_bytes, np.uint8)
        img = cv2.imdecode(np_img, cv2.IMREAD_COLOR)

        if img is None:
            logger.error("Could not decode image")
            return {
                "success": False,
                "message": "Could not decode image"
            }

        # ---- Detect face ----
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        faces = face_cascade.detectMultiScale(
            gray,
            scaleFactor=1.1,
            minNeighbors=5,
            minSize=(60, 60)
        )

        if len(faces) == 0:
            logger.warning("No face detected in image")
            return {
                "success": False,
                "message": "No face detected"
            }

        # Get the largest face and encode it
        x, y, w, h = max(faces, key=lambda f: f[2] * f[3])
        face_roi = gray[y:y + h, x:x + w]
        face_resized = cv2.resize(face_roi, (100, 100))
        unknown = face_resized.flatten().astype(float) / 255.0

        # ---- Compare with each known encoding (Euclidean distance) ----
        distances = [float(np.linalg.norm(unknown - k)) for k in known_encodings]
        best_index = int(np.argmin(distances))
        best_distance = float(distances[best_index])

        is_match = best_distance < MATCH_THRESHOLD

        # Confidence: 100% at distance 0, ~0% at CONFIDENCE_MAX_DISTANCE
        confidence = max(0.0, 100.0 * (1.0 - best_distance / CONFIDENCE_MAX_DISTANCE))

        logger.info(
            f"Best match: index={best_index}, distance={best_distance:.4f}, "
            f"is_match={is_match}, threshold={MATCH_THRESHOLD}, confidence={confidence:.1f}%"
        )

        return {
            "success": True,
            "best_index": best_index,
            "distance": best_distance,
            "is_match": is_match,
            "threshold": MATCH_THRESHOLD,
            "confidence": confidence
        }

    except Exception as e:
        logger.error(f"Error recognizing face: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Error processing image: {str(e)}")


# =========================
# /detect-faces (debug)
# =========================
@app.post("/detect-faces")
async def detect_faces(file: UploadFile = File(...)):
    """
    Detect all faces in an image and return their positions.
    Useful for debugging and testing.
    """
    try:
        image_bytes = await file.read()
        np_img = np.frombuffer(image_bytes, np.uint8)
        img = cv2.imdecode(np_img, cv2.IMREAD_COLOR)

        if img is None:
            return {
                "success": False,
                "message": "Could not decode image"
            }

        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        faces = face_cascade.detectMultiScale(
            gray,
            scaleFactor=1.1,
            minNeighbors=5,
            minSize=(60, 60)
        )

        face_list = []
        for (x, y, w, h) in faces:
            face_list.append({
                "x": int(x),
                "y": int(y),
                "width": int(w),
                "height": int(h)
            })

        logger.info(f"Detected {len(faces)} face(s)")

        return {
            "success": True,
            "faces_detected": len(faces),
            "faces": face_list,
            "image_width": img.shape[1],
            "image_height": img.shape[0]
        }

    except Exception as e:
        logger.error(f"Error detecting faces: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Error processing image: {str(e)}")


# =========================
# Local run (not used on Render if you use Docker CMD)
# =========================
if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", "8000"))
    logger.info(f"Starting Face Recognition API on port {port}...")
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=port,
        log_level="info"
    )
@app.get("/recognize-face")
async def recognize_face_get():
    return JSONResponse(
        status_code=405,
        content={"detail": "Use POST /recognize-face with an image and encodings."},
    )
