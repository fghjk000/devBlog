# DevBlog 아키텍처 재설계 구현 계획

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Spring Boot MPA → Spring Boot REST API + Next.js 14 SPA 완전 분리

**Architecture:** Spring Boot는 JSON REST API 서버로만 재구성 (Mustache/Thymeleaf 제거 + JWT 인증). Next.js 14 App Router가 프론트엔드 담당. Nginx가 `/api/*` 요청은 Spring Boot(:8080)로, 나머지는 Next.js(:3000)로 라우팅.

**Tech Stack:** Spring Boot 3.4.3 + Java 17, Next.js 14 App Router + TypeScript, Tailwind CSS, JWT (jjwt), Zustand, TanStack Query, Axios, Docker Compose, Nginx

---

## Phase 1: 백엔드 REST API 전환

---

### Task 1: 의존성 정리 및 JWT 추가

**Files:**
- Modify: `build.gradle`
- Modify: `src/main/resources/application.properties`
- Create: `.env.example`

**Step 1: build.gradle 수정 — Mustache/Thymeleaf 제거, JWT 추가**

```groovy
// 제거할 항목들:
// implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
// implementation 'org.thymeleaf.extras:thymeleaf-extras-springsecurity6'
// implementation 'org.springframework.boot:spring-boot-starter-mustache'

// 추가할 항목들:
implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'
```

최종 `build.gradle`:
```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.4.3'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.cos'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'
    compileOnly 'org.projectlombok:lombok'
    developmentOnly 'org.springframework.boot:spring-boot-devtools'
    runtimeOnly 'com.mysql:mysql-connector-j'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

**Step 2: application.properties 환경변수화**

```properties
spring.application.name=devBlog
server.port=8080
server.address=0.0.0.0
server.forward-headers-strategy=framework

spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/devBlog?useSSL=false&allowPublicKeyRetrieval=true}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:***REMOVED***}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=100MB
file.upload-dir=${FILE_UPLOAD_DIR:/app/uploads}

spring.mvc.hiddenmethod.filter.enabled=true
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.docker.compose.enabled=false

# JWT
jwt.secret=${JWT_SECRET:***REMOVED***}
jwt.access-token-expiration=900000
jwt.refresh-token-expiration=604800000

# CORS
cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:3000}

## Google OAuth2
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
spring.security.oauth2.client.registration.google.scope[0]=email
spring.security.oauth2.client.registration.google.scope[1]=profile
```

**Step 3: .env.example 생성**

```env
DB_URL=jdbc:mysql://mysql:3306/devBlog?useSSL=false&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your_db_password
JWT_SECRET=your-jwt-secret-key-must-be-at-least-256-bits
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
CORS_ALLOWED_ORIGINS=http://localhost:3000
FILE_UPLOAD_DIR=/app/uploads
```

**Step 4: Gradle 빌드 확인**

```bash
./gradlew dependencies --configuration runtimeClasspath | grep -E "jjwt|thymeleaf|mustache"
```
Expected: `jjwt-api`, `jjwt-impl`, `jjwt-jackson` 있음. thymeleaf, mustache 없음.

**Step 5: Commit**

```bash
git add build.gradle src/main/resources/application.properties .env.example
git commit -m "chore: JWT 의존성 추가, Thymeleaf/Mustache 제거, 환경변수화"
```

---

### Task 2: JWT 유틸리티 클래스 구현

**Files:**
- Create: `src/main/java/com/cos/devblog/config/jwt/JwtProperties.java`
- Create: `src/main/java/com/cos/devblog/config/jwt/JwtUtil.java`
- Test: `src/test/java/com/cos/devblog/config/jwt/JwtUtilTest.java`

**Step 1: 테스트 작성**

```java
// src/test/java/com/cos/devblog/config/jwt/JwtUtilTest.java
package com.cos.devblog.config.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void generateAndValidateToken() {
        String token = jwtUtil.generateAccessToken("user@test.com", "ROLE_USER");

        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.getEmail(token)).isEqualTo("user@test.com");
        assertThat(jwtUtil.getRole(token)).isEqualTo("ROLE_USER");
    }

    @Test
    void expiredTokenIsInvalid() {
        // 만료 시간 0으로 토큰 생성 → 즉시 만료
        String token = jwtUtil.generateTokenWithExpiry("user@test.com", "ROLE_USER", 0L);
        assertThat(jwtUtil.validateToken(token)).isFalse();
    }
}
```

**Step 2: 테스트 실패 확인**

```bash
./gradlew test --tests "com.cos.devblog.config.jwt.JwtUtilTest"
```
Expected: FAIL (JwtUtil class not found)

**Step 3: JwtProperties 구현**

```java
// src/main/java/com/cos/devblog/config/jwt/JwtProperties.java
package com.cos.devblog.config.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {
    private String secret;
    private long accessTokenExpiration;
    private long refreshTokenExpiration;
}
```

**Step 4: JwtUtil 구현**

```java
// src/main/java/com/cos/devblog/config/jwt/JwtUtil.java
package com.cos.devblog.config.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String email, String role) {
        return generateTokenWithExpiry(email, role, jwtProperties.getAccessTokenExpiration());
    }

    public String generateRefreshToken(String email, String role) {
        return generateTokenWithExpiry(email, role, jwtProperties.getRefreshTokenExpiration());
    }

    public String generateTokenWithExpiry(String email, String role, long expiryMs) {
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getEmail(String token) {
        return getClaims(token).getSubject();
    }

    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

**Step 5: 테스트 통과 확인**

```bash
./gradlew test --tests "com.cos.devblog.config.jwt.JwtUtilTest"
```
Expected: PASS (2 tests)

**Step 6: Commit**

```bash
git add src/main/java/com/cos/devblog/config/jwt/ src/test/java/com/cos/devblog/config/jwt/
git commit -m "feat: JWT 유틸리티 구현 (생성, 검증, 클레임 추출)"
```

---

### Task 3: JWT 인증 필터 구현

**Files:**
- Create: `src/main/java/com/cos/devblog/config/jwt/JwtAuthenticationFilter.java`
- Test: `src/test/java/com/cos/devblog/config/jwt/JwtAuthenticationFilterTest.java`

**Step 1: 테스트 작성**

```java
// src/test/java/com/cos/devblog/config/jwt/JwtAuthenticationFilterTest.java
package com.cos.devblog.config.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthenticationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void requestWithValidToken_shouldAuthenticate() throws Exception {
        String token = jwtUtil.generateAccessToken("admin@test.com", "ROLE_ADMIN");

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void requestWithoutToken_shouldReturn401ForProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }
}
```

**Step 2: JwtAuthenticationFilter 구현**

```java
// src/main/java/com/cos/devblog/config/jwt/JwtAuthenticationFilter.java
package com.cos.devblog.config.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                String email = jwtUtil.getEmail(token);
                String role = jwtUtil.getRole(token);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                List.of(new SimpleGrantedAuthority(role))
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

**Step 3: Commit (SecurityConfig 전환 후 테스트 실행)**

테스트는 Task 4 SecurityConfig 완성 후 실행.

```bash
git add src/main/java/com/cos/devblog/config/jwt/JwtAuthenticationFilter.java
git commit -m "feat: JWT 인증 필터 구현"
```

---

### Task 4: SecurityConfig REST API 전환

**Files:**
- Modify: `src/main/java/com/cos/devblog/config/SecurityConfig.java`

**Step 1: SecurityConfig 전체 교체**

```java
// src/main/java/com/cos/devblog/config/SecurityConfig.java
package com.cos.devblog.config;

import com.cos.devblog.config.jwt.JwtAuthenticationFilter;
import com.cos.devblog.config.jwt.JwtUtil;
import com.cos.devblog.config.oauth.PrincipalOauth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final PrincipalOauth2UserService principalOauth2UserService;
    private final JwtUtil jwtUtil;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public BCryptPasswordEncoder encoderPwd() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/comments/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/qna/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/chatbot/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/api/users/me").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil),
                        UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(principalOauth2UserService))
                        .successHandler((request, response, authentication) -> {
                            // Task 6에서 JWT 발급 로직 추가
                            response.sendRedirect("http://localhost:3000/auth/callback");
                        })
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        return new UrlBasedCorsConfigurationSource() {{
            registerCorsConfiguration("/**", config);
        }};
    }
}
```

**Step 2: 빌드 확인**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add src/main/java/com/cos/devblog/config/SecurityConfig.java
git commit -m "feat: SecurityConfig를 Stateless JWT 방식으로 전환"
```

---

### Task 5: 인증 API 구현 (login, register, refresh)

**Files:**
- Create: `src/main/java/com/cos/devblog/auth/dto/LoginRequest.java`
- Create: `src/main/java/com/cos/devblog/auth/dto/LoginResponse.java`
- Create: `src/main/java/com/cos/devblog/auth/controller/AuthController.java`
- Test: `src/test/java/com/cos/devblog/auth/controller/AuthControllerTest.java`

**Step 1: 테스트 작성**

```java
// src/test/java/com/cos/devblog/auth/controller/AuthControllerTest.java
package com.cos.devblog.auth.controller;

import com.cos.devblog.user.entity.User;
import com.cos.devblog.user.repository.UserRepository;
import com.cos.devblog.board.entity.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired BCryptPasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        User user = new User(null, "testuser", encoder.encode("password123"),
                "test@example.com", UserRole.ROLE_USER,
                LocalDateTime.now(), LocalDateTime.now());
        userRepository.save(user);
    }

    @Test
    void login_withValidCredentials_returnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "test@example.com", "password", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void login_withInvalidCredentials_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "test@example.com", "password", "wrongpassword"))))
                .andExpect(status().isUnauthorized());
    }
}
```

**Step 2: DTO 생성**

```java
// src/main/java/com/cos/devblog/auth/dto/LoginRequest.java
package com.cos.devblog.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @Email @NotBlank
    private String email;
    @NotBlank
    private String password;
}
```

```java
// src/main/java/com/cos/devblog/auth/dto/LoginResponse.java
package com.cos.devblog.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private String email;
    private String role;

    public LoginResponse(String accessToken, String email, String role) {
        this.accessToken = accessToken;
        this.email = email;
        this.role = role;
    }
}
```

**Step 3: AuthController 구현**

```java
// src/main/java/com/cos/devblog/auth/controller/AuthController.java
package com.cos.devblog.auth.controller;

import com.cos.devblog.auth.dto.LoginRequest;
import com.cos.devblog.auth.dto.LoginResponse;
import com.cos.devblog.config.jwt.JwtUtil;
import com.cos.devblog.user.entity.User;
import com.cos.devblog.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                                HttpServletResponse response) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null || !encoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String role = user.getRole().name();
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), role);
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail(), role);

        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/api/auth/refresh");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(refreshCookie);

        return ResponseEntity.ok(new LoginResponse(accessToken, user.getEmail(), role));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setMaxAge(0);
        cookie.setPath("/api/auth/refresh");
        response.addCookie(cookie);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null || !jwtUtil.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = jwtUtil.getEmail(refreshToken);
        String role = jwtUtil.getRole(refreshToken);
        String newAccessToken = jwtUtil.generateAccessToken(email, role);

        return ResponseEntity.ok(new LoginResponse(newAccessToken, email, role));
    }
}
```

**Step 4: UserRepository에 findByEmail 추가 확인**

`src/main/java/com/cos/devblog/user/repository/UserRepository.java`에 아래 메서드가 없으면 추가:
```java
Optional<User> findByEmail(String email);
```

**Step 5: 테스트 실행**

```bash
./gradlew test --tests "com.cos.devblog.auth.controller.AuthControllerTest"
```
Expected: PASS (2 tests)

**Step 6: Commit**

```bash
git add src/main/java/com/cos/devblog/auth/ src/test/java/com/cos/devblog/auth/
git commit -m "feat: 인증 REST API 구현 (login, logout, refresh)"
```

---

### Task 6: Post REST API 전환

**Files:**
- Create: `src/main/java/com/cos/devblog/board/controller/PostApiController.java`
- Create: `src/main/java/com/cos/devblog/board/dto/PostResponse.java`
- Create: `src/main/java/com/cos/devblog/board/dto/PostRequest.java`
- Test: `src/test/java/com/cos/devblog/board/controller/PostApiControllerTest.java`

**Step 1: 테스트 작성**

```java
// src/test/java/com/cos/devblog/board/controller/PostApiControllerTest.java
package com.cos.devblog.board.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PostApiControllerTest {

    @Autowired MockMvc mockMvc;

    @Test
    void getPostList_returnsJson() throws Exception {
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    void getPostList_withPagination() throws Exception {
        mockMvc.perform(get("/api/posts?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }
}
```

**Step 2: DTO 생성**

```java
// src/main/java/com/cos/devblog/board/dto/PostResponse.java
package com.cos.devblog.board.dto;

import com.cos.devblog.board.entity.Post;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PostResponse {
    private Long id;
    private String title;
    private String content;
    private String author;
    private String imageUrl;
    private String categoryName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int commentCount;

    public static PostResponse from(Post post) {
        PostResponse dto = new PostResponse();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setAuthor(post.getAuthor());
        dto.setImageUrl(post.getImageUrl());
        dto.setCategoryName(post.getCategory() != null ? post.getCategory().getName() : null);
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());
        dto.setCommentCount(post.getComments() != null ? post.getComments().size() : 0);
        return dto;
    }
}
```

**Step 3: PostApiController 구현**

```java
// src/main/java/com/cos/devblog/board/controller/PostApiController.java
package com.cos.devblog.board.controller;

import com.cos.devblog.board.dto.PostResponse;
import com.cos.devblog.board.entity.Post;
import com.cos.devblog.board.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostApiController {

    private final PostService postService;

    @GetMapping
    public ResponseEntity<Page<PostResponse>> getPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));
        Page<Post> posts = search != null && !search.isBlank()
                ? postService.searchByTitle(search, pageable)
                : postService.getAllPosts(pageable);

        return ResponseEntity.ok(posts.map(PostResponse::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(PostResponse.from(postService.getDetail(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PostResponse> createPost(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) MultipartFile image) throws IOException {
        postService.savePost(title, content, image);
        return ResponseEntity.status(201).build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) MultipartFile image) throws IOException {
        postService.updatePost(id, title, content, image);
        return ResponseEntity.ok(PostResponse.from(postService.getDetail(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }
}
```

**Step 4: 기존 PostController 삭제**

`src/main/java/com/cos/devblog/board/controller/PostController.java` 파일 삭제.

**Step 5: 테스트 실행**

```bash
./gradlew test --tests "com.cos.devblog.board.controller.PostApiControllerTest"
```
Expected: PASS

**Step 6: Commit**

```bash
git add src/main/java/com/cos/devblog/board/ src/test/java/com/cos/devblog/board/
git commit -m "feat: Post REST API 구현 (/api/posts)"
```

---

### Task 7: Category, Comment, Like API 구현

**Files:**
- Create: `src/main/java/com/cos/devblog/board/controller/CategoryApiController.java`
- Create: `src/main/java/com/cos/devblog/board/controller/CommentApiController.java`
- Create: `src/main/java/com/cos/devblog/board/controller/LikeApiController.java`
- 기존 삭제: `CategoryController.java`, `CommentController.java`

**Step 1: CategoryApiController**

```java
// src/main/java/com/cos/devblog/board/controller/CategoryApiController.java
package com.cos.devblog.board.controller;

import com.cos.devblog.board.entity.Category;
import com.cos.devblog.board.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryApiController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<Category>> getCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }
}
```

**Step 2: Comment DTO 및 ApiController**

```java
// src/main/java/com/cos/devblog/board/dto/CommentRequest.java
package com.cos.devblog.board.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentRequest {
    @NotBlank
    private String content;
    private Long postId;
}
```

```java
// src/main/java/com/cos/devblog/board/controller/CommentApiController.java
package com.cos.devblog.board.controller;

import com.cos.devblog.board.dto.CommentRequest;
import com.cos.devblog.board.entity.Comment;
import com.cos.devblog.board.service.CommentService;
import com.cos.devblog.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentApiController {

    private final CommentService commentService;
    private final UserRepository userRepository;

    @GetMapping("/{postId}")
    public ResponseEntity<List<Comment>> getComments(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.getComments(postId));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Comment> createComment(
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal String email) {
        Comment comment = commentService.saveComment(request.getContent(), request.getPostId(), email);
        return ResponseEntity.status(201).body(comment);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }
}
```

**Step 3: CommentService에 saveComment(content, postId, email) 메서드 추가**

기존 `CommentService`를 열고, email로 User를 조회해서 댓글 저장하는 메서드를 추가한다.

```java
// CommentService.java에 추가
@Autowired
private UserRepository userRepository;

public Comment saveComment(String content, Long postId, String email) {
    Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Post not found"));
    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

    Comment comment = new Comment();
    comment.setContent(content);
    comment.setPost(post);
    comment.setUser(user);
    comment.setCreatedAt(LocalDateTime.now());
    return commentRepository.save(comment);
}

public void deleteComment(Long id) {
    commentRepository.deleteById(id);
}
```

**Step 4: 기존 컨트롤러 삭제 및 Commit**

`CategoryController.java`, `CommentController.java` 삭제.

```bash
git add src/main/java/com/cos/devblog/board/
git commit -m "feat: Category, Comment REST API 구현"
```

---

### Task 8: QnA REST API 전환

**Files:**
- Create: `src/main/java/com/cos/devblog/qna/controller/QnaApiController.java`
- Create: `src/main/java/com/cos/devblog/qna/dto/QnaRequest.java`
- Create: `src/main/java/com/cos/devblog/qna/dto/QnaResponse.java`
- 기존 삭제: `QnaController.java`, `AnswerController.java`

**Step 1: QnaResponse DTO**

```java
// src/main/java/com/cos/devblog/qna/dto/QnaResponse.java
package com.cos.devblog.qna.dto;

import com.cos.devblog.qna.entity.Qna;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class QnaResponse {
    private Long id;
    private String title;
    private String content;
    private String username;
    private LocalDateTime createdAt;
    private boolean hasAnswer;

    public static QnaResponse from(Qna qna) {
        QnaResponse dto = new QnaResponse();
        dto.setId(qna.getId());
        dto.setTitle(qna.getTitle());
        dto.setContent(qna.getContent());
        dto.setUsername(qna.getUsername());
        dto.setCreatedAt(qna.getCreatedAt());
        dto.setHasAnswer(!qna.getAnswers().isEmpty());
        return dto;
    }
}
```

**Step 2: QnaApiController**

```java
// src/main/java/com/cos/devblog/qna/controller/QnaApiController.java
package com.cos.devblog.qna.controller;

import com.cos.devblog.qna.dto.QnaResponse;
import com.cos.devblog.qna.entity.Qna;
import com.cos.devblog.qna.service.QnaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/qna")
@RequiredArgsConstructor
public class QnaApiController {

    private final QnaService qnaService;

    @GetMapping
    public ResponseEntity<Page<QnaResponse>> getQnaList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));
        return ResponseEntity.ok(qnaService.getAllQna(pageable).map(QnaResponse::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QnaResponse> getQna(@PathVariable Long id) {
        return ResponseEntity.ok(QnaResponse.from(qnaService.getQna(id)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<QnaResponse> createQna(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal String email) {
        Qna qna = qnaService.createQna(body.get("title"), body.get("content"), email);
        return ResponseEntity.status(201).body(QnaResponse.from(qna));
    }

    @PostMapping("/{id}/answer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> createAnswer(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal String email) {
        qnaService.createAnswer(id, body.get("content"), email);
        return ResponseEntity.status(201).build();
    }
}
```

**Step 3: QnaService 메서드 추가/수정**

기존 `QnaService`에 `getAllQna(Pageable)`, `getQna(Long)`, `createQna(...)`, `createAnswer(...)` 메서드가 없으면 추가한다.

**Step 4: 기존 컨트롤러 삭제 및 Commit**

```bash
git add src/main/java/com/cos/devblog/qna/
git commit -m "feat: QnA REST API 구현 (/api/qna)"
```

---

### Task 9: User API 및 Chatbot API

**Files:**
- Create: `src/main/java/com/cos/devblog/user/controller/UserApiController.java`
- Create: `src/main/java/com/cos/devblog/board/controller/ChatbotApiController.java`
- 기존 삭제: `LoginController.java`, `HomeController.java`

**Step 1: UserApiController**

```java
// src/main/java/com/cos/devblog/user/controller/UserApiController.java
package com.cos.devblog.user.controller;

import com.cos.devblog.user.entity.User;
import com.cos.devblog.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserApiController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMe(@AuthenticationPrincipal String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "role", user.getRole().name()
        ));
    }
}
```

**Step 2: ChatbotApiController**

```java
// src/main/java/com/cos/devblog/board/controller/ChatbotApiController.java
package com.cos.devblog.board.controller;

import com.cos.devblog.board.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotApiController {

    private final ChatbotService chatbotService;

    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> ask(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        String answer = chatbotService.ask(question);
        return ResponseEntity.ok(Map.of("answer", answer));
    }
}
```

**Step 3: templates 폴더 삭제**

```bash
rm -rf src/main/resources/templates/
```

**Step 4: 전체 빌드 및 테스트**

```bash
./gradlew test
```
Expected: BUILD SUCCESSFUL (모든 테스트 PASS)

**Step 5: Commit**

```bash
git add src/main/java/com/cos/devblog/
git commit -m "feat: User, Chatbot REST API 구현, 템플릿 파일 제거"
```

---

## Phase 2: Next.js 프론트엔드 구축

---

### Task 10: Next.js 프로젝트 초기화

**Files:**
- Create: `frontend/` (새 Next.js 프로젝트)

**Step 1: Next.js 프로젝트 생성**

```bash
cd /Users/kimhanseop/Desktop/devBlog
npx create-next-app@latest frontend \
  --typescript \
  --tailwind \
  --eslint \
  --app \
  --src-dir \
  --import-alias "@/*"
```
프롬프트:
- Would you like to use Turbopack? → No

**Step 2: 추가 패키지 설치**

```bash
cd frontend
npm install axios zustand @tanstack/react-query
npm install -D @types/node
```

**Step 3: 환경변수 파일 생성**

```bash
# frontend/.env.local
NEXT_PUBLIC_API_URL=http://localhost:8080
```

**Step 4: 프로젝트 실행 확인**

```bash
npm run dev
```
Expected: http://localhost:3000 접속 가능

**Step 5: Commit**

```bash
cd ..
git add frontend/
git commit -m "feat: Next.js 14 프론트엔드 프로젝트 초기화"
```

---

### Task 11: API 클라이언트 및 인증 상태 관리

**Files:**
- Create: `frontend/src/lib/api.ts`
- Create: `frontend/src/store/authStore.ts`
- Create: `frontend/src/types/index.ts`

**Step 1: 공통 타입 정의**

```typescript
// frontend/src/types/index.ts
export interface Post {
  id: number;
  title: string;
  content: string;
  author: string;
  imageUrl: string | null;
  categoryName: string | null;
  createdAt: string;
  updatedAt: string | null;
  commentCount: number;
}

export interface Comment {
  id: number;
  content: string;
  createdAt: string;
  user: { username: string };
}

export interface Qna {
  id: number;
  title: string;
  content: string;
  username: string;
  createdAt: string;
  hasAnswer: boolean;
}

export interface Category {
  id: number;
  name: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface AuthUser {
  email: string;
  role: string;
  accessToken: string;
}
```

**Step 2: Axios 인스턴스**

```typescript
// frontend/src/lib/api.ts
import axios from 'axios';

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL,
  withCredentials: true, // 쿠키 (refreshToken) 포함
});

// 요청 인터셉터: 메모리에서 accessToken 첨부
api.interceptors.request.use((config) => {
  if (typeof window !== 'undefined') {
    const token = sessionStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

// 응답 인터셉터: 401 시 refresh 시도
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        const res = await axios.post(
          `${process.env.NEXT_PUBLIC_API_URL}/api/auth/refresh`,
          {},
          { withCredentials: true }
        );
        const { accessToken } = res.data;
        sessionStorage.setItem('accessToken', accessToken);
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return api(originalRequest);
      } catch {
        sessionStorage.removeItem('accessToken');
        window.location.href = '/auth/login';
      }
    }
    return Promise.reject(error);
  }
);

export default api;
```

**Step 3: Zustand 인증 스토어**

```typescript
// frontend/src/store/authStore.ts
import { create } from 'zustand';
import { AuthUser } from '@/types';

interface AuthState {
  user: AuthUser | null;
  setUser: (user: AuthUser | null) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  setUser: (user) => {
    set({ user });
    if (user) {
      sessionStorage.setItem('accessToken', user.accessToken);
    }
  },
  logout: () => {
    set({ user: null });
    sessionStorage.removeItem('accessToken');
  },
}));
```

**Step 4: TanStack Query Provider 설정**

```typescript
// frontend/src/app/providers.tsx
'use client';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useState } from 'react';

export default function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(() => new QueryClient({
    defaultOptions: {
      queries: { staleTime: 60 * 1000 },
    },
  }));

  return (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );
}
```

```typescript
// frontend/src/app/layout.tsx 수정 — Providers 래핑 추가
import Providers from './providers';
// ... children을 <Providers>{children}</Providers>로 감싸기
```

**Step 5: Commit**

```bash
cd frontend
git add src/lib/ src/store/ src/types/ src/app/providers.tsx src/app/layout.tsx
git commit -m "feat: API 클라이언트, Zustand 인증 스토어, TanStack Query 설정"
```

---

### Task 12: 공통 레이아웃 (Header, Footer)

**Files:**
- Create: `frontend/src/components/layout/Header.tsx`
- Create: `frontend/src/components/layout/Footer.tsx`
- Modify: `frontend/src/app/layout.tsx`

**Step 1: Header 컴포넌트**

```tsx
// frontend/src/components/layout/Header.tsx
'use client';

import Link from 'next/link';
import { useAuthStore } from '@/store/authStore';
import api from '@/lib/api';
import { useRouter } from 'next/navigation';

export default function Header() {
  const { user, logout } = useAuthStore();
  const router = useRouter();

  const handleLogout = async () => {
    await api.post('/api/auth/logout');
    logout();
    router.push('/');
  };

  return (
    <header className="bg-white shadow-sm sticky top-0 z-50">
      <div className="max-w-4xl mx-auto px-4 py-4 flex items-center justify-between">
        <Link href="/" className="text-xl font-bold text-gray-900">
          DevBlog
        </Link>
        <nav className="flex items-center gap-6 text-sm">
          <Link href="/posts" className="text-gray-600 hover:text-gray-900">글 목록</Link>
          <Link href="/qna" className="text-gray-600 hover:text-gray-900">Q&A</Link>
          {user ? (
            <>
              {user.role === 'ROLE_ADMIN' && (
                <Link href="/posts/new" className="text-blue-600 hover:text-blue-800">글 쓰기</Link>
              )}
              <Link href="/mypage" className="text-gray-600 hover:text-gray-900">마이페이지</Link>
              <button onClick={handleLogout} className="text-red-500 hover:text-red-700">
                로그아웃
              </button>
            </>
          ) : (
            <Link href="/auth/login" className="text-blue-600 hover:text-blue-800">로그인</Link>
          )}
        </nav>
      </div>
    </header>
  );
}
```

**Step 2: Footer 컴포넌트**

```tsx
// frontend/src/components/layout/Footer.tsx
export default function Footer() {
  return (
    <footer className="bg-gray-50 border-t mt-16">
      <div className="max-w-4xl mx-auto px-4 py-8 text-center text-sm text-gray-500">
        © 2026 DevBlog. Built with Next.js & Spring Boot.
      </div>
    </footer>
  );
}
```

**Step 3: layout.tsx에 Header/Footer 추가**

```tsx
// frontend/src/app/layout.tsx
import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import './globals.css';
import Providers from './providers';
import Header from '@/components/layout/Header';
import Footer from '@/components/layout/Footer';

const inter = Inter({ subsets: ['latin'] });

export const metadata: Metadata = {
  title: 'DevBlog',
  description: '개발 블로그',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body className={inter.className}>
        <Providers>
          <Header />
          <main className="min-h-screen">{children}</main>
          <Footer />
        </Providers>
      </body>
    </html>
  );
}
```

**Step 4: Commit**

```bash
git add src/components/ src/app/layout.tsx
git commit -m "feat: Header, Footer 공통 레이아웃 컴포넌트 구현"
```

---

### Task 13: 홈 페이지 및 게시글 목록 페이지

**Files:**
- Modify: `frontend/src/app/page.tsx`
- Create: `frontend/src/app/posts/page.tsx`
- Create: `frontend/src/components/post/PostCard.tsx`

**Step 1: PostCard 컴포넌트**

```tsx
// frontend/src/components/post/PostCard.tsx
import Link from 'next/link';
import { Post } from '@/types';

export default function PostCard({ post }: { post: Post }) {
  return (
    <article className="border rounded-lg overflow-hidden hover:shadow-md transition-shadow">
      {post.imageUrl && (
        <div className="aspect-video overflow-hidden">
          <img
            src={`${process.env.NEXT_PUBLIC_API_URL}${post.imageUrl}`}
            alt={post.title}
            className="w-full h-full object-cover"
          />
        </div>
      )}
      <div className="p-4">
        {post.categoryName && (
          <span className="text-xs text-blue-600 font-medium">{post.categoryName}</span>
        )}
        <h2 className="mt-1 text-lg font-semibold text-gray-900 line-clamp-2">
          <Link href={`/posts/${post.id}`}>{post.title}</Link>
        </h2>
        <div className="mt-2 flex items-center gap-3 text-xs text-gray-500">
          <span>{new Date(post.createdAt).toLocaleDateString('ko-KR')}</span>
          <span>댓글 {post.commentCount}</span>
        </div>
      </div>
    </article>
  );
}
```

**Step 2: 홈 페이지 (최근 글 5개)**

```tsx
// frontend/src/app/page.tsx
import Link from 'next/link';
import PostCard from '@/components/post/PostCard';
import { Post, PageResponse } from '@/types';

async function getRecentPosts(): Promise<Post[]> {
  const res = await fetch(
    `${process.env.NEXT_PUBLIC_API_URL}/api/posts?page=0&size=5`,
    { next: { revalidate: 60 } }
  );
  const data: PageResponse<Post> = await res.json();
  return data.content;
}

export default async function Home() {
  const posts = await getRecentPosts();

  return (
    <div className="max-w-4xl mx-auto px-4 py-12">
      <section>
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold text-gray-900">최근 글</h1>
          <Link href="/posts" className="text-sm text-blue-600 hover:text-blue-800">
            전체 보기 →
          </Link>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {posts.map((post) => (
            <PostCard key={post.id} post={post} />
          ))}
        </div>
      </section>
    </div>
  );
}
```

**Step 3: 게시글 목록 페이지**

```tsx
// frontend/src/app/posts/page.tsx
import PostCard from '@/components/post/PostCard';
import { Post, PageResponse } from '@/types';

async function getPosts(page: number): Promise<PageResponse<Post>> {
  const res = await fetch(
    `${process.env.NEXT_PUBLIC_API_URL}/api/posts?page=${page}&size=10`,
    { next: { revalidate: 60 } }
  );
  return res.json();
}

export default async function PostsPage({
  searchParams,
}: {
  searchParams: { page?: string };
}) {
  const page = Number(searchParams.page ?? 0);
  const data = await getPosts(page);

  return (
    <div className="max-w-4xl mx-auto px-4 py-12">
      <h1 className="text-2xl font-bold mb-8">게시글</h1>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {data.content.map((post) => (
          <PostCard key={post.id} post={post} />
        ))}
      </div>
      {/* 페이지네이션 */}
      <div className="flex justify-center gap-2 mt-8">
        {Array.from({ length: data.totalPages }, (_, i) => (
          <a
            key={i}
            href={`/posts?page=${i}`}
            className={`px-3 py-1 rounded border text-sm ${
              i === page ? 'bg-blue-600 text-white border-blue-600' : 'hover:bg-gray-50'
            }`}
          >
            {i + 1}
          </a>
        ))}
      </div>
    </div>
  );
}
```

**Step 4: Commit**

```bash
git add src/app/page.tsx src/app/posts/ src/components/post/
git commit -m "feat: 홈 페이지, 게시글 목록 페이지 구현"
```

---

### Task 14: 게시글 상세 페이지

**Files:**
- Create: `frontend/src/app/posts/[id]/page.tsx`
- Create: `frontend/src/components/comment/CommentSection.tsx`

**Step 1: 게시글 상세 페이지 (SSR)**

```tsx
// frontend/src/app/posts/[id]/page.tsx
import { Post, Comment } from '@/types';
import CommentSection from '@/components/comment/CommentSection';

async function getPost(id: string): Promise<Post> {
  const res = await fetch(
    `${process.env.NEXT_PUBLIC_API_URL}/api/posts/${id}`,
    { cache: 'no-store' }
  );
  if (!res.ok) throw new Error('Post not found');
  return res.json();
}

async function getComments(id: string): Promise<Comment[]> {
  const res = await fetch(
    `${process.env.NEXT_PUBLIC_API_URL}/api/comments/${id}`,
    { cache: 'no-store' }
  );
  return res.json();
}

export default async function PostDetailPage({ params }: { params: { id: string } }) {
  const [post, comments] = await Promise.all([
    getPost(params.id),
    getComments(params.id),
  ]);

  return (
    <div className="max-w-3xl mx-auto px-4 py-12">
      <article>
        {post.imageUrl && (
          <img
            src={`${process.env.NEXT_PUBLIC_API_URL}${post.imageUrl}`}
            alt={post.title}
            className="w-full rounded-lg mb-8 max-h-96 object-cover"
          />
        )}
        <div className="mb-4">
          {post.categoryName && (
            <span className="text-sm text-blue-600">{post.categoryName}</span>
          )}
          <h1 className="text-3xl font-bold mt-2">{post.title}</h1>
          <p className="text-sm text-gray-500 mt-2">
            {new Date(post.createdAt).toLocaleDateString('ko-KR')}
          </p>
        </div>
        <div
          className="prose max-w-none mt-8"
          dangerouslySetInnerHTML={{ __html: post.content }}
        />
      </article>

      <CommentSection postId={Number(params.id)} initialComments={comments} />
    </div>
  );
}
```

**Step 2: CommentSection 컴포넌트 (클라이언트)**

```tsx
// frontend/src/components/comment/CommentSection.tsx
'use client';

import { useState } from 'react';
import { Comment } from '@/types';
import { useAuthStore } from '@/store/authStore';
import api from '@/lib/api';

export default function CommentSection({
  postId,
  initialComments,
}: {
  postId: number;
  initialComments: Comment[];
}) {
  const [comments, setComments] = useState<Comment[]>(initialComments);
  const [content, setContent] = useState('');
  const { user } = useAuthStore();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!content.trim()) return;

    const res = await api.post('/api/comments', { content, postId });
    setComments([...comments, res.data]);
    setContent('');
  };

  return (
    <section className="mt-12 border-t pt-8">
      <h2 className="text-xl font-semibold mb-6">댓글 {comments.length}개</h2>

      <ul className="space-y-4 mb-8">
        {comments.map((comment) => (
          <li key={comment.id} className="flex gap-3">
            <div className="flex-1">
              <div className="flex items-center gap-2 mb-1">
                <span className="font-medium text-sm">{comment.user?.username ?? '익명'}</span>
                <span className="text-xs text-gray-400">
                  {new Date(comment.createdAt).toLocaleDateString('ko-KR')}
                </span>
              </div>
              <p className="text-gray-700">{comment.content}</p>
            </div>
          </li>
        ))}
      </ul>

      {user ? (
        <form onSubmit={handleSubmit} className="flex gap-2">
          <input
            type="text"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="댓글을 입력하세요"
            className="flex-1 border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <button
            type="submit"
            className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700"
          >
            등록
          </button>
        </form>
      ) : (
        <p className="text-sm text-gray-500">댓글을 작성하려면 로그인하세요.</p>
      )}
    </section>
  );
}
```

**Step 3: Commit**

```bash
git add src/app/posts/[id]/ src/components/comment/
git commit -m "feat: 게시글 상세 페이지, 댓글 섹션 구현"
```

---

### Task 15: 로그인 페이지

**Files:**
- Create: `frontend/src/app/auth/login/page.tsx`
- Create: `frontend/src/app/auth/callback/page.tsx`

**Step 1: 로그인 페이지**

```tsx
// frontend/src/app/auth/login/page.tsx
'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/store/authStore';
import api from '@/lib/api';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const { setUser } = useAuthStore();
  const router = useRouter();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      const res = await api.post('/api/auth/login', { email, password });
      setUser(res.data);
      router.push('/');
    } catch {
      setError('이메일 또는 비밀번호가 올바르지 않습니다.');
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="bg-white p-8 rounded-xl shadow-sm w-full max-w-sm">
        <h1 className="text-2xl font-bold text-center mb-8">로그인</h1>

        {error && (
          <p className="text-red-500 text-sm text-center mb-4">{error}</p>
        )}

        <form onSubmit={handleLogin} className="space-y-4">
          <input
            type="email"
            placeholder="이메일"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <input
            type="password"
            placeholder="비밀번호"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <button
            type="submit"
            className="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 font-medium"
          >
            로그인
          </button>
        </form>

        <div className="mt-4">
          <a
            href={`${process.env.NEXT_PUBLIC_API_URL}/oauth2/authorization/google`}
            className="w-full flex items-center justify-center gap-2 border rounded-lg py-2 hover:bg-gray-50 text-sm"
          >
            Google로 로그인
          </a>
        </div>
      </div>
    </div>
  );
}
```

**Step 2: OAuth2 콜백 페이지**

```tsx
// frontend/src/app/auth/callback/page.tsx
'use client';

import { useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useAuthStore } from '@/store/authStore';

export default function AuthCallbackPage() {
  const searchParams = useSearchParams();
  const { setUser } = useAuthStore();
  const router = useRouter();

  useEffect(() => {
    const token = searchParams.get('token');
    const email = searchParams.get('email');
    const role = searchParams.get('role');

    if (token && email && role) {
      setUser({ accessToken: token, email, role });
      router.push('/');
    } else {
      router.push('/auth/login');
    }
  }, [searchParams, setUser, router]);

  return (
    <div className="min-h-screen flex items-center justify-center">
      <p className="text-gray-500">로그인 처리 중...</p>
    </div>
  );
}
```

**Step 3: SecurityConfig OAuth2 successHandler 업데이트**

Task 4의 SecurityConfig에서 successHandler를 아래처럼 수정:

```java
.successHandler((request, response, authentication) -> {
    String email = authentication.getName();
    String role = authentication.getAuthorities().iterator().next().getAuthority();
    String token = jwtUtil.generateAccessToken(email, role);
    response.sendRedirect(
        "http://localhost:3000/auth/callback?token=" + token
        + "&email=" + email + "&role=" + role
    );
})
```

**Step 4: middleware.ts — 보호 라우트**

```typescript
// frontend/src/middleware.ts
import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

const PROTECTED_PATHS = ['/mypage', '/posts/new'];

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const isProtected = PROTECTED_PATHS.some((path) => pathname.startsWith(path));

  if (isProtected) {
    // sessionStorage는 서버에서 접근 불가 → 클라이언트에서 리다이렉트 처리
    // 여기서는 쿠키 기반으로 간단히 체크 (refreshToken 존재 여부)
    const refreshToken = request.cookies.get('refreshToken');
    if (!refreshToken) {
      return NextResponse.redirect(new URL('/auth/login', request.url));
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/mypage/:path*', '/posts/new/:path*'],
};
```

**Step 5: Commit**

```bash
git add src/app/auth/ src/middleware.ts
git commit -m "feat: 로그인 페이지, OAuth2 콜백, 라우트 보호 미들웨어 구현"
```

---

### Task 16: QnA 페이지 및 마이페이지

**Files:**
- Create: `frontend/src/app/qna/page.tsx`
- Create: `frontend/src/app/qna/[id]/page.tsx`
- Create: `frontend/src/app/mypage/page.tsx`

**Step 1: QnA 목록 페이지**

```tsx
// frontend/src/app/qna/page.tsx
import Link from 'next/link';
import { Qna, PageResponse } from '@/types';

async function getQnaList(page: number): Promise<PageResponse<Qna>> {
  const res = await fetch(
    `${process.env.NEXT_PUBLIC_API_URL}/api/qna?page=${page}&size=10`,
    { cache: 'no-store' }
  );
  return res.json();
}

export default async function QnaPage({ searchParams }: { searchParams: { page?: string } }) {
  const page = Number(searchParams.page ?? 0);
  const data = await getQnaList(page);

  return (
    <div className="max-w-4xl mx-auto px-4 py-12">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-2xl font-bold">Q&A</h1>
        <Link href="/qna/new" className="text-sm bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700">
          질문하기
        </Link>
      </div>

      <ul className="divide-y">
        {data.content.map((qna) => (
          <li key={qna.id} className="py-4">
            <Link href={`/qna/${qna.id}`} className="flex items-center justify-between hover:text-blue-600">
              <div>
                <div className="flex items-center gap-2">
                  {qna.hasAnswer ? (
                    <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded">답변완료</span>
                  ) : (
                    <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded">미답변</span>
                  )}
                  <h2 className="font-medium">{qna.title}</h2>
                </div>
                <p className="text-sm text-gray-500 mt-1">
                  {qna.username} · {new Date(qna.createdAt).toLocaleDateString('ko-KR')}
                </p>
              </div>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
```

**Step 2: 마이페이지**

```tsx
// frontend/src/app/mypage/page.tsx
'use client';

import { useEffect, useState } from 'react';
import { useAuthStore } from '@/store/authStore';
import { useRouter } from 'next/navigation';
import api from '@/lib/api';

export default function MyPage() {
  const { user } = useAuthStore();
  const router = useRouter();
  const [me, setMe] = useState<{ username: string; email: string; role: string } | null>(null);

  useEffect(() => {
    if (!user) {
      router.push('/auth/login');
      return;
    }
    api.get('/api/users/me').then((res) => setMe(res.data));
  }, [user, router]);

  if (!me) return <div className="flex justify-center py-20 text-gray-500">로딩 중...</div>;

  return (
    <div className="max-w-2xl mx-auto px-4 py-12">
      <h1 className="text-2xl font-bold mb-8">마이페이지</h1>
      <div className="bg-white rounded-xl border p-6 space-y-4">
        <div className="flex justify-between py-2 border-b">
          <span className="text-gray-500">이름</span>
          <span className="font-medium">{me.username}</span>
        </div>
        <div className="flex justify-between py-2 border-b">
          <span className="text-gray-500">이메일</span>
          <span className="font-medium">{me.email}</span>
        </div>
        <div className="flex justify-between py-2">
          <span className="text-gray-500">권한</span>
          <span className="font-medium">{me.role === 'ROLE_ADMIN' ? '관리자' : '일반 사용자'}</span>
        </div>
      </div>
    </div>
  );
}
```

**Step 3: Commit**

```bash
git add src/app/qna/ src/app/mypage/
git commit -m "feat: QnA 목록, 마이페이지 구현"
```

---

## Phase 3: Docker Compose 및 배포 설정

---

### Task 17: Next.js Dockerfile 및 Docker Compose 업데이트

**Files:**
- Create: `frontend/Dockerfile`
- Modify: `compose.yaml`
- Modify: `nginx.conf`
- Create: `.env` (gitignore에 추가)

**Step 1: Next.js Dockerfile**

```dockerfile
# frontend/Dockerfile
FROM node:20-alpine AS base
WORKDIR /app

FROM base AS deps
COPY package*.json ./
RUN npm ci

FROM base AS builder
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ENV NEXT_TELEMETRY_DISABLED=1
RUN npm run build

FROM base AS runner
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs
EXPOSE 3000
CMD ["node", "server.js"]
```

`frontend/next.config.ts`에 standalone output 추가:
```typescript
const nextConfig = {
  output: 'standalone',
};
export default nextConfig;
```

**Step 2: compose.yaml 업데이트**

```yaml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: devBlog
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  spring:
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      DB_URL: jdbc:mysql://mysql:3306/devBlog?useSSL=false&allowPublicKeyRetrieval=true
      DB_USERNAME: ${DB_USERNAME:-root}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID}
      GOOGLE_CLIENT_SECRET: ${GOOGLE_CLIENT_SECRET}
      CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS:-http://localhost:3000}
      FILE_UPLOAD_DIR: /app/uploads
    volumes:
      - uploads:/app/uploads
    depends_on:
      mysql:
        condition: service_healthy

  nextjs:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    ports:
      - "3000:3000"
    environment:
      NEXT_PUBLIC_API_URL: ${NEXT_PUBLIC_API_URL:-http://localhost:8080}
    depends_on:
      - spring

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - spring
      - nextjs

volumes:
  mysql_data:
  uploads:
```

**Step 3: nginx.conf 업데이트**

```nginx
events {
    worker_connections 1024;
}

http {
    upstream spring {
        server spring:8080;
    }

    upstream nextjs {
        server nextjs:3000;
    }

    server {
        listen 80;

        location /api/ {
            proxy_pass http://spring;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        location /oauth2/ {
            proxy_pass http://spring;
            proxy_set_header Host $host;
        }

        location /login/ {
            proxy_pass http://spring;
            proxy_set_header Host $host;
        }

        location /uploads/ {
            proxy_pass http://spring;
        }

        location / {
            proxy_pass http://nextjs;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection 'upgrade';
        }
    }
}
```

**Step 4: .env 파일 생성 및 .gitignore 확인**

`.gitignore`에 `.env` 추가 (없으면):
```
.env
blog_chatbot/.env
```

**Step 5: 전체 Docker Compose 빌드 테스트**

```bash
docker compose build
```
Expected: 모든 서비스 빌드 성공

**Step 6: Commit**

```bash
git add frontend/Dockerfile frontend/next.config.ts compose.yaml nginx.conf .env.example .gitignore
git commit -m "feat: Next.js Dockerfile, Docker Compose 업데이트, Nginx 라우팅 설정"
```

---

### Task 18: 통합 테스트 및 최종 검증

**Step 1: 로컬 통합 실행**

```bash
# 1. Spring Boot 백엔드 실행
./gradlew bootRun

# 2. Next.js 프론트엔드 실행 (별도 터미널)
cd frontend && npm run dev
```

**Step 2: 주요 시나리오 수동 테스트**

- [ ] http://localhost:3000 → 홈 화면, 최근 글 로드
- [ ] http://localhost:3000/posts → 게시글 목록
- [ ] http://localhost:3000/posts/1 → 게시글 상세 + 댓글
- [ ] http://localhost:3000/auth/login → 로그인, JWT 발급
- [ ] Google OAuth2 로그인 → 콜백 처리 → 홈 리다이렉트
- [ ] 로그인 후 댓글 작성
- [ ] http://localhost:3000/mypage → 내 정보 조회
- [ ] http://localhost:3000/qna → QnA 목록

**Step 3: Docker Compose 전체 실행 테스트**

```bash
docker compose up -d
```

접속 확인:
- http://localhost → Nginx 통해 Next.js
- http://localhost/api/posts → Nginx 통해 Spring Boot

**Step 4: 최종 커밋**

```bash
git add .
git commit -m "feat: DevBlog v2 - Spring Boot REST API + Next.js 아키텍처 재설계 완료"
```

---

## 요약

| Phase | Tasks | 주요 변경 |
|-------|-------|---------|
| Phase 1 | 1-9 | Spring Boot REST API 전환, JWT 인증, Thymeleaf 제거 |
| Phase 2 | 10-16 | Next.js 14 프론트엔드 구축 |
| Phase 3 | 17-18 | Docker Compose 업데이트, 통합 테스트 |

**총 커밋 수**: 약 15개 (태스크당 1-2개)

> 작업 시작 전: `git checkout -b feature/architecture-redesign`으로 작업 브랜치 생성 권장
