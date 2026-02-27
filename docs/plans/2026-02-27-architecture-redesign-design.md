# DevBlog 아키텍처 재설계 설계 문서

**날짜**: 2026-02-27
**목적**: 포트폴리오 / 통합 역량 전시용
**접근법**: 풀 분리 (Spring Boot REST API + Next.js)

---

## 1. 개요

현재 Spring Boot + Mustache/Thymeleaf MPA 구조를 Next.js 프론트엔드 + Spring Boot REST API 백엔드로 완전히 분리한다.

**현재 스택**:
- Spring Boot 3.4.3 + Mustache + Thymeleaf (MPA)
- Spring Security (세션 기반)
- Google OAuth2
- MySQL
- Docker Compose + Nginx

**목표 스택**:
- **백엔드**: Spring Boot 3.4.3 REST API (JSON only)
- **프론트엔드**: Next.js 14 App Router + TypeScript + Tailwind CSS
- **인증**: JWT (Access Token + Refresh Token) + Google OAuth2
- **배포**: Docker Compose (MySQL, Spring Boot, Next.js, Nginx)

---

## 2. 전체 아키텍처

```
┌─────────────────────────────────────────┐
│              브라우저                    │
└──────────────┬──────────────────────────┘
               │
        ┌──────▼──────┐
        │    Nginx     │  (Reverse Proxy)
        │  :80 / :443  │
        └──┬───────┬───┘
           │       │
    /api/* │       │ /*
           │       │
  ┌────────▼──┐  ┌─▼───────────┐
  │Spring Boot│  │  Next.js    │
  │  :8080    │  │   :3000     │
  │ REST API  │  │ SSR/SSG     │
  └────────┬──┘  └─────────────┘
           │
    ┌──────▼──────┐
    │    MySQL    │
    │   :3306     │
    └─────────────┘
```

---

## 3. 백엔드 변경 사항

### 3.1 제거
- `spring-boot-starter-mustache` 의존성
- `spring-boot-starter-thymeleaf` 의존성
- `thymeleaf-extras-springsecurity6` 의존성
- 기존 HTML 반환 MVC 컨트롤러
- `src/main/resources/templates/` 폴더 전체

### 3.2 추가
- `jjwt-api`, `jjwt-impl`, `jjwt-jackson` → JWT 발급/검증
- `@RestController` 기반 API 컨트롤러 (기존 서비스 레이어 재활용)
- JWT 필터 (`JwtAuthenticationFilter`)
- Spring Security Stateless 설정

### 3.3 REST API 명세

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/api/auth/login` | 이메일/비밀번호 로그인 | X |
| POST | `/api/auth/refresh` | Access Token 갱신 | Refresh Token 쿠키 |
| POST | `/api/auth/logout` | 로그아웃 | O |
| GET | `/api/posts` | 게시글 목록 (페이지네이션) | X |
| POST | `/api/posts` | 게시글 작성 | O (ADMIN) |
| GET | `/api/posts/{id}` | 게시글 상세 | X |
| PUT | `/api/posts/{id}` | 게시글 수정 | O (ADMIN) |
| DELETE | `/api/posts/{id}` | 게시글 삭제 | O (ADMIN) |
| GET | `/api/categories` | 카테고리 목록 | X |
| POST | `/api/categories` | 카테고리 생성 | O (ADMIN) |
| GET | `/api/comments/{postId}` | 댓글 목록 | X |
| POST | `/api/comments` | 댓글 작성 | O |
| DELETE | `/api/comments/{id}` | 댓글 삭제 | O |
| POST | `/api/posts/{id}/like` | 좋아요 토글 | O |
| GET | `/api/qna` | QnA 목록 | X |
| POST | `/api/qna` | QnA 등록 | O |
| GET | `/api/qna/{id}` | QnA 상세 | X |
| POST | `/api/qna/{id}/answer` | 답변 작성 | O (ADMIN) |
| POST | `/api/chatbot/ask` | 챗봇 질문 | X |
| GET | `/api/users/me` | 내 정보 조회 | O |

### 3.4 보안 조치
- `application.properties` 시크릿 → 환경변수 (`JWT_SECRET`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`)
- CORS: Next.js 도메인만 허용
- Access Token: 15분 만료
- Refresh Token: 7일 만료, httpOnly 쿠키로 전달

---

## 4. 프론트엔드 구조 (Next.js 14)

```
frontend/
├── app/
│   ├── layout.tsx              # 공통 레이아웃 (헤더, 푸터)
│   ├── page.tsx                # 홈 (최근 글 목록)
│   ├── auth/
│   │   ├── login/page.tsx      # 로그인 페이지
│   │   └── callback/page.tsx   # OAuth2 콜백 처리
│   ├── posts/
│   │   ├── page.tsx            # 게시글 목록 (SSG/ISR)
│   │   ├── [id]/page.tsx       # 게시글 상세 (SSR)
│   │   ├── new/page.tsx        # 글 작성 (Protected)
│   │   └── [id]/edit/page.tsx  # 글 수정 (Protected)
│   ├── qna/
│   │   ├── page.tsx            # QnA 목록
│   │   └── [id]/page.tsx       # QnA 상세
│   └── mypage/page.tsx         # 내 페이지 (Protected)
├── components/
│   ├── ui/                     # 기본 UI 컴포넌트 (Button, Input 등)
│   ├── layout/                 # Header, Footer, Sidebar
│   ├── post/                   # PostCard, PostEditor 등
│   └── comment/                # CommentList, CommentForm
├── lib/
│   ├── api.ts                  # axios 인스턴스 + JWT interceptor
│   └── auth.ts                 # 토큰 관리 유틸
├── store/
│   └── authStore.ts            # Zustand 인증 상태
├── types/
│   └── index.ts                # 공통 TypeScript 타입
└── middleware.ts                # 인증 필요 라우트 보호
```

**주요 기술**:
- TypeScript
- Tailwind CSS
- Zustand (클라이언트 인증 상태)
- TanStack Query (서버 상태 캐싱)
- Axios (HTTP 클라이언트)

---

## 5. 인증 흐름

### 5.1 일반 로그인
```
1. 클라이언트 → POST /api/auth/login {email, password}
2. 서버 → Access Token (응답 body) + Refresh Token (httpOnly 쿠키)
3. 클라이언트 → Access Token을 메모리(Zustand)에 저장
4. API 요청 시 Authorization: Bearer <access_token> 헤더 첨부
5. Access Token 만료 시 → POST /api/auth/refresh 자동 호출
```

### 5.2 Google OAuth2 로그인
```
1. 클라이언트 → /oauth2/authorization/google 리다이렉트
2. Google 인증 완료 → Spring Boot 콜백
3. Spring Boot → JWT 발급 → Next.js /auth/callback?token=... 리다이렉트
4. Next.js → 토큰 저장 및 홈으로 이동
```

---

## 6. Docker Compose 구성

```yaml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: devBlog
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}

  spring:
    build: .
    ports: ["8080:8080"]
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/devBlog
      JWT_SECRET: ${JWT_SECRET}
      GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID}
      GOOGLE_CLIENT_SECRET: ${GOOGLE_CLIENT_SECRET}
    depends_on: [mysql]

  nextjs:
    build: ./frontend
    ports: ["3000:3000"]
    environment:
      NEXT_PUBLIC_API_URL: http://spring:8080

  nginx:
    image: nginx:alpine
    ports: ["80:80"]
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
    depends_on: [spring, nextjs]
```

---

## 7. 마이그레이션 순서

1. **백엔드 REST API 전환**
   - 의존성 정리 (Mustache/Thymeleaf 제거, JWT 추가)
   - SecurityConfig 수정 (Stateless)
   - 기존 컨트롤러 → `@RestController` 전환
   - JWT 필터 구현
   - 환경변수 분리

2. **Next.js 프로젝트 생성**
   - `frontend/` 디렉토리에 Next.js 14 프로젝트 초기화
   - API 클라이언트 구현 (axios + JWT interceptor)
   - 인증 상태 관리 (Zustand)
   - 페이지 컴포넌트 구현

3. **Docker Compose 업데이트**
   - Next.js 서비스 추가
   - 환경변수 `.env` 파일로 관리
   - Nginx 설정 업데이트

4. **통합 테스트 및 검증**
