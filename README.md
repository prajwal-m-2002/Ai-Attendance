# 🤖 AI Attendance System

> **Full-stack, AI-powered attendance management system** using real-time face recognition — built with Python FastAPI, Spring Boot, OpenCV, and PostgreSQL. Deployable via Docker on cloud platforms like Render.

[![Python](https://img.shields.io/badge/Python-3.11-blue?logo=python)](https://python.org)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.100+-green?logo=fastapi)](https://fastapi.tiangolo.com)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![OpenCV](https://img.shields.io/badge/OpenCV-4.x-red?logo=opencv)](https://opencv.org)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Neon-blue?logo=postgresql)](https://postgresql.org)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue?logo=docker)](https://docker.com)

---

## 🔥 Features

- 🎯 **Real-time Face Detection** — OpenCV Haar Cascade detects faces from uploaded images
- 🧠 **Face Encoding & Matching** — Euclidean-distance-based recognition with configurable confidence threshold
- ✅ **Automatic Attendance Marking** — Spring Boot backend marks and stores attendance per student
- 🗄️ **Database Persistence** — Student records and attendance logs stored in PostgreSQL (Neon cloud)
- 🔌 **Microservice Architecture** — Decoupled Python Face Recognition API + Java Spring Boot backend
- 🐳 **Dockerized** — Both services containerized and production-ready for Render.com deployment
- 📊 **REST API** — Clean API surface for integrating any front-end or mobile app

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Face Recognition | Python 3.11, OpenCV, NumPy |
| API Gateway | FastAPI + Uvicorn |
| Backend | Java 17, Spring Boot 3.2.5 |
| ORM | Spring Data JPA / Hibernate |
| Database | PostgreSQL (Neon cloud) |
| Templating | Thymeleaf |
| Containerization | Docker |
| Deployment | Render.com |

---

## 📐 System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                      Client / Browser                    │
└────────────────────────┬────────────────────────────────┘
                         │ HTTP
┌────────────────────────▼────────────────────────────────┐
│           Spring Boot Backend  (Java 17)                 │
│  - Student Controller   - Attendance Controller          │
│  - JPA Repositories     - Thymeleaf Views               │
└───────────┬─────────────────────────────────────────────┘
            │ REST (multipart image + encodings JSON)
┌───────────▼─────────────────────────────────────────────┐
│         Python FastAPI  (Face Recognition Service)       │
│  POST /encode-face    →  generates 10,000-dim vector     │
│  POST /recognize-face →  Euclidean distance matching     │
│  POST /detect-faces   →  bounding box debug endpoint     │
└───────────┬─────────────────────────────────────────────┘
            │
┌───────────▼─────────────────────────────────────────────┐
│         OpenCV  (Haar Cascade face detection)            │
└─────────────────────────────────────────────────────────┘
```

---

## 📂 Project Structure

```
Ai-Attendance-main/
│
├── python-face-recognition/          # Face recognition microservice
│   ├── app.py                        # FastAPI app (encode, recognize, detect)
│   ├── requirements.txt              # Python dependencies
│   └── Dockerfile                    # Docker config for Render deployment
│
├── spring-backend/
│   └── demo/
│       ├── src/main/java/com/attendance/
│       │   ├── DemoApplication.java          # Spring Boot entry point
│       │   ├── config/AppConfig.java         # App configuration (CORS, beans)
│       │   ├── controller/
│       │   │   ├── AttendancePageController  # Attendance marking logic
│       │   │   ├── StudentController.java    # Student registration & encoding
│       │   │   └── PageController.java       # Thymeleaf page routing
│       │   ├── model/
│       │   │   ├── Student.java              # Student entity (face encoding stored)
│       │   │   └── Attendance.java           # Attendance record entity
│       │   └── repository/
│       │       ├── StudentRepository.java
│       │       └── AttendanceRepository.java
│       ├── pom.xml                           # Maven build config
│       └── Dockerfile                        # Docker config for Spring service
│
├── .gitignore
└── README.md
```

---

## ⚙️ How to Run Locally

### Prerequisites
- Python 3.11+
- Java 17+
- Maven 3.8+
- PostgreSQL (local or [Neon free tier](https://neon.tech))

### 1️⃣ Start the Python Face Recognition Service

```bash
cd python-face-recognition
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8000 --reload
```

API docs available at: `http://localhost:8000/docs`

### 2️⃣ Configure the Spring Boot Backend

Edit `spring-backend/demo/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://<YOUR_DB_HOST>/<YOUR_DB>
spring.datasource.username=<username>
spring.datasource.password=<password>
face.recognition.api.url=http://localhost:8000
```

### 3️⃣ Start the Spring Boot Backend

```bash
cd spring-backend/demo
./mvnw spring-boot:run
```

App runs at: `http://localhost:8080`

---

## 🐳 Docker Deployment (Render / Cloud)

Each service has its own `Dockerfile`. Deploy independently on Render:

```bash
# Face recognition service
cd python-face-recognition
docker build -t face-recognition-api .
docker run -p 8000:8000 face-recognition-api

# Spring Boot backend
cd spring-backend/demo
docker build -t attendance-backend .
docker run -p 8080:8080 attendance-backend
```

Set environment variables on Render:
- `MATCH_THRESHOLD` — face matching threshold (default: `25.0`)
- `CONFIDENCE_MAX_DISTANCE` — distance for 0% confidence (default: `40.0`)
- `DATABASE_URL` — PostgreSQL connection string

---

## 🔌 API Reference

### Face Recognition Service (Python FastAPI)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/` | Health check |
| GET | `/health` | Detailed health status |
| POST | `/encode-face` | Encode face from image → returns 10K-dim vector |
| POST | `/recognize-face` | Compare image against known encodings |
| POST | `/detect-faces` | Detect and return bounding boxes (debug) |

**Example — Encode Face:**
```bash
curl -X POST "http://localhost:8000/encode-face" \
  -F "file=@student_photo.jpg"
```

**Example — Recognize Face:**
```bash
curl -X POST "http://localhost:8000/recognize-face" \
  -F "file=@face.jpg" \
  -F 'known_encodings_json=[[0.1, 0.2, ...], [0.3, 0.4, ...]]'
```

---

## 🗄️ Database Schema

```
students
├── id (PK)
├── name
├── roll_number
└── face_encoding (stored as JSON array)

attendance
├── id (PK)
├── student_id (FK → students)
├── timestamp
└── status (PRESENT / ABSENT)
```

---

## 🚀 Future Improvements

- [ ] **Web Dashboard** — Real-time attendance view with charts (React/Thymeleaf)
- [ ] **Live Camera Feed** — Stream webcam directly from browser for real-time recognition
- [ ] **Email Reports** — Scheduled attendance summaries via email
- [ ] **Excel Export** — Download attendance logs as `.xlsx`
- [ ] **Deep Learning Upgrade** — Replace OpenCV Haar Cascade with FaceNet/ArcFace for higher accuracy
- [ ] **Mobile App** — Flutter/React Native client
- [ ] **Role-based Auth** — JWT-secured admin and teacher roles
- [ ] **Multi-face Support** — Recognize multiple students in one frame

---

## 👤 Author

**Prajwal**
- GitHub: https://github.com/prajwal-m-2002
- LinkedIn: https://www.linkedin.com/in/prajwalm2002/
---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
