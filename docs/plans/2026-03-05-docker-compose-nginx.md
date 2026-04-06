# Docker Compose + Nginx 구성 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Spring Boot + Next.js + Nginx를 docker-compose로 묶어, nginx가 호스트 3000번 포트에서 프론트를 서빙하고 `/uploads/`를 spring-boot로 프록시하도록 구성. 프론트 이미지 URL을 `https://blog.haruseop.com/uploads/`로 변경.

**Architecture:** nginx(호스트:3000) → Next.js(내부:3000) / Spring Boot(내부:8080) / MySQL(내부:3306). `/uploads/`는 nginx에서 `spring-boot:8080`으로 내부 Docker 네트워크를 통해 프록시. 프론트는 빌드 타임에 `NEXT_PUBLIC_API_URL=https://blog.haruseop.com`을 주입받아 이미지 절대 경로 생성.

**Tech Stack:** Docker Compose v2, Nginx Alpine, Next.js 16 (standalone), Spring Boot (Gradle), MySQL 8

---

### Task 1: `docker-compose.yml` 생성

**Files:**
- Create: `docker-compose.yml`

**Step 1: `docker-compose.yml` 작성**

```yaml
services:
  mysql:
    image: mysql:8
    container_name: devblog-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD:-devblog123}
      MYSQL_DATABASE: devBlog
    volumes:
      - mysql_data:/var/lib/mysql
    networks:
      - app_network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${DB_PASSWORD:-devblog123}"]
      interval: 5s
      timeout: 5s
      retries: 20
      start_period: 30s

  spring-boot:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: devblog-spring
    environment:
      DB_URL: jdbc:mysql://mysql:3306/devBlog?useSSL=false&allowPublicKeyRetrieval=true
      DB_USERNAME: root
      DB_PASSWORD: ${DB_PASSWORD:-devblog123}
      FILE_UPLOAD_DIR: /app/uploads
      CORS_ALLOWED_ORIGINS: https://blog.haruseop.com
    volumes:
      - uploads_data:/app/uploads
    networks:
      - app_network
    depends_on:
      mysql:
        condition: service_healthy
    restart: on-failure

  nextjs:
    build:
      context: ./frontend
      dockerfile: Dockerfile
      args:
        NEXT_PUBLIC_API_URL: https://blog.haruseop.com
    container_name: devblog-nextjs
    networks:
      - app_network
    depends_on:
      - spring-boot

  nginx:
    image: nginx:alpine
    container_name: devblog-nginx
    ports:
      - "3000:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
    networks:
      - app_network
    depends_on:
      - nextjs
      - spring-boot
    restart: unless-stopped

volumes:
  mysql_data:
  uploads_data:

networks:
  app_network:
    driver: bridge
```

**Step 2: 파일 저장 확인**

```bash
cat docker-compose.yml
```
Expected: 파일 내용 출력됨

**Step 3: docker-compose 문법 검증**

```bash
docker compose config
```
Expected: 오류 없이 완성된 설정이 출력됨

**Step 4: 커밋**

```bash
git add docker-compose.yml
git commit -m "feat: add docker-compose with nginx, nextjs, spring-boot, mysql"
```

---

### Task 2: `frontend/Dockerfile`에 `NEXT_PUBLIC_API_URL` 빌드 인자 추가

**Files:**
- Modify: `frontend/Dockerfile:8` (builder 스테이지)

현재 `frontend/Dockerfile`의 builder 스테이지:
```dockerfile
FROM base AS builder
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ENV NEXT_TELEMETRY_DISABLED=1
RUN npm run build
```

**Step 1: builder 스테이지에 ARG/ENV 추가**

`FROM base AS builder` 바로 다음 줄에 ARG와 ENV를 추가:

```dockerfile
FROM base AS builder
ARG NEXT_PUBLIC_API_URL
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ENV NEXT_TELEMETRY_DISABLED=1
RUN npm run build
```

> **중요:** `ARG`와 `ENV` 설정이 반드시 `RUN npm run build` 보다 앞에 있어야 빌드 타임에 주입됨. Next.js `NEXT_PUBLIC_*` 변수는 런타임이 아닌 빌드 타임에 번들링됨.

**Step 2: 변경 확인**

```bash
head -20 frontend/Dockerfile
```
Expected: `ARG NEXT_PUBLIC_API_URL`, `ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL` 라인이 builder 스테이지 내에 보임

**Step 3: 커밋**

```bash
git add frontend/Dockerfile
git commit -m "feat: inject NEXT_PUBLIC_API_URL as build arg in frontend Dockerfile"
```

---

### Task 3: `nginx.conf` `/uploads/` 프록시 확인 및 보완

**Files:**
- Modify: `nginx.conf` (필요 시)

현재 `nginx.conf`의 `/uploads/` 설정:
```nginx
location /uploads/ {
    proxy_pass http://spring;
}
```

`upstream spring` 블록:
```nginx
upstream spring {
    server spring-boot:8080;
}
```

**Step 1: 현재 nginx.conf 확인**

```bash
cat nginx.conf
```
Expected: `/uploads/` location이 `http://spring` (= `spring-boot:8080`)으로 프록시되어 있음

**Step 2: proxy_set_header 추가 (없을 경우)**

`/uploads/` location에 헤더가 없다면 아래처럼 보완:

```nginx
location /uploads/ {
    proxy_pass http://spring;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
```

현재 이미 올바르게 설정되어 있다면 변경 불필요.

**Step 3: 커밋 (변경사항 있을 경우에만)**

```bash
git add nginx.conf
git commit -m "fix: add proxy headers for /uploads/ location"
```

---

### Task 4: 전체 스택 통합 테스트

**Step 1: Docker 이미지 빌드**

```bash
docker compose build
```
Expected: 오류 없이 모든 이미지 빌드 완료 (시간 소요됨)

**Step 2: 전체 스택 실행**

```bash
docker compose up -d
```
Expected: 4개 컨테이너 모두 `Started` 또는 `Running` 상태

**Step 3: 컨테이너 상태 확인**

```bash
docker compose ps
```
Expected: devblog-mysql, devblog-spring, devblog-nextjs, devblog-nginx 모두 `running`

**Step 4: nginx 응답 확인**

```bash
curl -I http://localhost:3000
```
Expected: `HTTP/1.1 200 OK` 또는 Next.js 응답

**Step 5: `/uploads/` 프록시 확인**

```bash
curl -I http://localhost:3000/uploads/test.png
```
Expected: `404 Not Found` (spring-boot에서 응답) 또는 실제 파일이면 `200 OK`. `502 Bad Gateway`가 나오면 spring-boot 컨테이너 상태 재확인.

**Step 6: 이미지 URL 확인 (브라우저)**

`http://localhost:3000` 접속 후 게시글의 이미지 `src` 속성 확인.
Expected: `https://blog.haruseop.com/uploads/...` 형태

**Step 7: 로그 확인 (문제 발생 시)**

```bash
docker compose logs nginx
docker compose logs spring-boot
docker compose logs nextjs
```

**Step 8: 스택 종료**

```bash
docker compose down
```

---

### Task 5: 최종 커밋 및 정리

**Step 1: 변경사항 전체 확인**

```bash
git status
git diff HEAD
```

**Step 2: 최종 커밋**

```bash
git add .
git commit -m "feat: docker-compose setup with nginx port 3000, uploads proxy to spring-boot"
```
