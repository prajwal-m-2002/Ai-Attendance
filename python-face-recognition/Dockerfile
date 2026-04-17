# Lightweight Python image
FROM python:3.11-slim

# Workdir inside the container
WORKDIR /app

# Install OS dependencies required by OpenCV (headless)
RUN apt-get update && apt-get install -y \
    libgl1 \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

# Copy and install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application source
COPY . .

# Expose default port (Render uses $PORT env var)
EXPOSE 8000

# Use Render's PORT env var with fallback to 8000
CMD ["sh", "-c", "uvicorn app:app --host 0.0.0.0 --port ${PORT:-8000}"]
