# 🤖 AI-Powered Attendance System

## 🚀 Overview
An AI-powered attendance system that uses real-time face recognition to automatically mark attendance and eliminate proxy entries.

## 🏗️ System Architecture
Camera → Face Detection → Face Recognition → Database → Attendance Dashboard

## ⚙️ How It Works
1. Capture image using webcam
2. Detect face using OpenCV
3. Extract facial features
4. Compare with stored dataset
5. Mark attendance with timestamp

## 🛠️ Tech Stack
- Python
- OpenCV
- face_recognition
- PostgreSQL / Spring Boot (Can substitute MySQL / Firebase)

## 🛑 Problem Statement
Traditional attendance systems are slow and prone to proxy buddy-punching. This system replaces manual tracking with an automated, zero-touch experience, saving administrative hours and guaranteeing data accuracy.

## ✨ Key Features
- **Real-Time Processing:** Instantly processes faces and logs them securely.
- **Proxy Prevention:** Eliminates fake attendance entries.
- **Microservices Design:** Decoupled AI application and robust Java backend.
- **Docker-Ready Deployments:** Containerized for clean cloud deployments on Render.

## 🎥 Demo
*(Add screenshots or video here)*

### 🖥️ Admin Dashboard
![Dashboard](docs/screenshots/dashboard.png)

### 📋 Attendance Logs
![Attendance Log](docs/screenshots/attendance.png)

### 🔌 AI Service API Docs
![Face Recognition API Docs](docs/screenshots/api_docs.png)

## 🚀 Installation & Setup

### 1. Launch Face Recognition API
```bash
cd python-face-recognition
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8000
```

### 2. Launch Backend Database Server
Make sure to update `application.properties` with your DB credentials.
```bash
cd spring-backend/demo
./mvnw spring-boot:run
```

## 👨‍💻 Author
**Prajwal**
- **GitHub:** [prajwal-m-2002](https://github.com/prajwal-m-2002)
- **LinkedIn:** [Prajwal M](https://www.linkedin.com/in/prajwalm2002/)
