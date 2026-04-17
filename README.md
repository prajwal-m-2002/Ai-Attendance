# 🤖 AI-Powered Attendance System

## 🚀 Overview
An AI-powered attendance system that uses real-time face recognition to automatically mark attendance, eliminating manual tracking and proxy entries.

## 🎥 Demo
**▶️ [Watch the Full Video Walkthrough Here](https://youtube.com/your-video-link-here)** *(Add your YouTube/Drive link here!)*

### 🖥️ Admin Dashboard
![Dashboard](docs/screenshots/dashboard.png)

### 📋 Attendance Logs
![Attendance Log](docs/screenshots/attendance.png)

### 🔌 AI Service API Integration
![Face Recognition API Docs](docs/screenshots/api_docs.png)

## 🏗️ System Architecture
`Camera` ➔ `Face Detection` ➔ `Face Recognition` ➔ `Database` ➔ `Attendance Dashboard`

## 🧠 Technical Details
- **Face Detection** using OpenCV (Haar Cascades for rapid bounding box detection)
- **Face Recognition** using the `face_recognition` library (powered by dlib's state-of-the-art CNN/HOG models mapping precise facial encodings)
- **Real-time processing** using webcam frames processed via a decoupled Python FastAPI microservice
- **Attendance stored** securely in a PostgreSQL database with exact timestamps via a Java Spring Boot backend

## ⚙️ How It Works
1. Capture image from webcam
2. Detect face in frame
3. Extract facial features
4. Match with stored dataset
5. Mark attendance automatically

## 🛠️ Tech Stack
- **AI & Computer Vision:** Python, OpenCV, `face_recognition`
- **Backend APIs:** Java, Spring Boot, FastAPI
- **Database:** PostgreSQL (Cloud deployment / Neon)
- **Deployment:** Docker & Render

## 🚀 Installation & Setup

### 1. Launch Face Recognition System
```bash
cd python-face-recognition
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8000
```

### 2. Launch Backend Database Server
Make sure to update `application.properties` with your PostgreSQL DB credentials.
```bash
cd spring-backend/demo
./mvnw spring-boot:run
```

## 👨‍💻 Author
**Prajwal**
- **GitHub:** [prajwal-m-2002](https://github.com/prajwal-m-2002)
- **LinkedIn:** [Prajwal M](https://www.linkedin.com/in/prajwalm2002/)

---
*Built to redefine operational efficiency using scalable artificial intelligence.*
