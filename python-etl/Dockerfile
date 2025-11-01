# Build stage with Python dependencies
FROM python:3.11-slim as builder

# Install system dependencies
RUN apt-get update && apt-get install -y \
    build-essential \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy requirements first for better caching
COPY requirements.txt .

# Install Python dependencies
RUN pip install --no-cache-dir --user -r requirements.txt

# Runtime stage
FROM python:3.11-slim

# Install runtime dependencies
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Create app directory
WORKDIR /app

# Create logs and data directories
RUN mkdir -p /app/logs /app/data

# Copy Python packages from builder
COPY --from=builder /root/.local /root/.local

# Copy source code
COPY src/ ./src/
COPY tests/ ./tests/

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser
RUN chown -R appuser:appuser /app
USER appuser

# Set Python path
ENV PYTHONPATH=/app/src
ENV PATH=/root/.local/bin:$PATH

# Default command
CMD ["python", "src/ingest_wikidata.py"]