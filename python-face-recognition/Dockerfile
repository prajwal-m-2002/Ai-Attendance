# Lightweight Python image
FROM python:3.11-slim

# Workdir inside the container
WORKDIR /app

# Install OS dependencies required by OpenCV
RUN apt-get update && apt-get install -y \
    libgl1 \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

# Copy and install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy the rest of the app (including app.py)
COPY . .

# Expose the port FastAPI will listen on
EXPOSE 8000

# Start FastAPI with Uvicorn
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000"]
# Expose the port FastAPI will listen on (optional for Render)
EXPOSE 8000

# IMPORTANT: use Render's PORT env var, not a hard-coded 5000/8000
CMD ["sh", "-c", "uvicorn app:app --host 0.0.0.0 --port ${PORT:-8000}"]
