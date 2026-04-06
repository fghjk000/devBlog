# Docker Compose + Nginx 설계 문서

**날짜**: 2026-03-05
**브랜치**: feature/architecture-redesign

## 목표

Spring Boot 서버 + Next.js 프론트엔드 + Nginx를 docker-compose로 묶어 배포 환경 구성.

## 요구사항

1. `docker-compose.yml` — nginx, nextjs, spring-boot, mysql 서비스 통합
2. nginx → 호스트 3000번 포트로 연결
3. nginx → 프론트엔드 리버스 프록시 (`/` → nextjs:3000)
4. nginx → `/uploads` 엔드포인트를 `spring-boot:8080/uploads`로 리버스 프록시
5. 프론트에서 이미지 호출 경로를 `https://blog.haruseop.com/uploads/...`로 변경

## 아키텍처

```
호스트:3000 → nginx (컨테이너:80)
                ├── /              → nextjs:3000      (내부 Docker 네트워크)
                ├── /api/          → spring-boot:8080 (내부 Docker 네트워크)
                ├── /oauth2/       → spring-boot:8080
                ├── /login/        → spring-boot:8080
                └── /uploads/      → spring-boot:8080 (= 호스트의 localhost:8080)
```

## 변경 파일

### 1. `docker-compose.yml` (신규 생성)

| 서비스 | 이미지 | 포트 | 비고 |
|---|---|---|---|
| nginx | nginx:alpine | 호스트 3000 → 컨테이너 80 | nginx.conf 마운트 |
| nextjs | 빌드 from `./frontend` | 내부 3000 | NEXT_PUBLIC_API_URL 빌드 인자 |
| spring-boot | 빌드 from `./` | 내부 8080 | mysql healthcheck 후 시작 |
| mysql | mysql:8 | 내부 3306 | uploads_data 볼륨 공유 |

- 볼륨: `mysql_data`, `uploads_data`
- 네트워크: `app_network` (bridge)
- 시작 순서: mysql → spring-boot → nginx, nextjs

### 2. `frontend/Dockerfile` (수정)

builder 스테이지에 빌드 인자 추가:
```dockerfile
ARG NEXT_PUBLIC_API_URL
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL
```

### 3. `nginx.conf` (변경 없음)

현재 `/uploads/` → `spring-boot:8080` 라우팅이 이미 올바르게 구성되어 있음.

## 이미지 URL 변경 방식

`PostCard.tsx:13`에서 이미지 src를 `${NEXT_PUBLIC_API_URL}${post.imageUrl}`로 조합 중.
`Post.getImageUrl()`은 `/uploads/filename`을 반환.

따라서 `docker-compose.yml`의 nextjs 빌드 인자에서:
```yaml
NEXT_PUBLIC_API_URL: https://blog.haruseop.com
```
→ 최종 URL: `https://blog.haruseop.com/uploads/filename`
