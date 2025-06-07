# Python 3.12.2 기반 슬림 이미지 사용
FROM python:3.12.2-slim

# 작업 디렉토리 설정
WORKDIR /app

# 시스템 패키지 설치 (konlpy 등 한글 처리를 위한 Java 포함)
RUN apt-get update && \
    apt-get install -y curl openjdk-17-jre-headless g++ && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# requirements.txt 복사 및 패키지 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 전체 앱 복사
COPY . .

# FastAPI 서버 실행 (app.py 기준)
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8080"]
