# Spring Security 인증 시스템 구조 및 동작 원리

## 📋 목차

1. [개요](#개요)
2. [아키텍처 구조](#아키텍처-구조)
3. [주요 컴포넌트](#주요-컴포넌트)
4. [인증 흐름](#인증-흐름)
5. [설정 상세](#설정-상세)
6. [데이터베이스 스키마](#데이터베이스-스키마)
7. [사용 방법](#사용-방법)

---

## 개요

본 프로젝트는 **JSESSIONID 기반 세션 인증**을 사용하는 Spring Security 구현입니다.
DB에 저장된 사용자 정보를 기반으로 인증을 수행하며, 모든 인증된 사용자에게 `ROLE_USER` 권한을 부여합니다.

### 주요 특징

- ✅ JSESSIONID 쿠키 기반 세션 관리
- ✅ DB 기반 사용자 인증 (`Users` 엔티티)
- ✅ BCrypt 비밀번호 암호화
- ✅ CORS 설정 통합
- ✅ 폼 로그인 지원 (`/login`)
- ✅ 로그인 성공 시 `/` 리다이렉트

---

## 아키텍처 구조

```
┌─────────────────────────────────────────────────────────────┐
│                    Client (Browser)                         │
└────────────────────┬────────────────────────────────────────┘
                     │ HTTP Request
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              Spring Security Filter Chain                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  1. CORS Filter (CorsConfig)                         │  │
│  │  2. CSRF Filter (Disabled)                           │  │
│  │  3. Authentication Filter                            │  │
│  │  4. Authorization Filter                             │  │
│  │  5. Session Management                               │  │
│  └──────────────────────────────────────────────────────┘  │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              SecurityConfig                                 │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  - SecurityFilterChain                               │  │
│  │  - PasswordEncoder (BCrypt)                          │  │
│  └──────────────────────────────────────────────────────┘  │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│         CustomUserDetailsService (@Service)                 │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  loadUserByUsername(String username)                 │  │
│  │    ↓                                                  │  │
│  │  UsersRepository.findByUserLoginId()                 │  │
│  │    ↓                                                  │  │
│  │  CustomUserDetails.from(Users)                       │  │
│  └──────────────────────────────────────────────────────┘  │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              UsersRepository (JPA)                          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  findByUserLoginId(String userLoginId)               │  │
│  └──────────────────────────────────────────────────────┘  │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              Database (users table)                         │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  user_id (PK)                                        │  │
│  │  user_login_id (UNIQUE)                              │  │
│  │  user_password (BCrypt)                              │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## 주요 컴포넌트

### 1. SecurityConfig (`config/SecurityConfig.java`)

**역할**: Spring Security의 전체 설정을 담당하는 핵심 설정 클래스

**주요 Bean**:

- `PasswordEncoder`: BCrypt 알고리즘을 사용한 비밀번호 암호화
- `SecurityFilterChain`: HTTP 요청에 대한 보안 필터 체인 설정

**설정 내용**:

```java
- CSRF: 비활성화 (쿠폰 발급 API 특성 고려)
- CORS: CorsConfig의 Bean 사용
- 세션 정책: IF_REQUIRED (필요 시 JSESSIONID 생성)
- URL 접근 제어:
  * /login, 정적 리소스: permitAll()
  * 그 외 모든 요청: hasRole("USER")
- 폼 로그인:
  * 로그인 페이지: /login
  * 처리 URL: /login
  * 성공 시: / 로 리다이렉트
- 로그아웃:
  * URL: /logout
  * 성공 시: /login?logout
  * JSESSIONID 쿠키 삭제
```

---

### 2. CustomUserDetailsService (`security/CustomUserDetailsService.java`)

**역할**: DB에서 사용자 정보를 조회하여 Spring Security가 사용할 수 있는 형태로 변환

**구현 인터페이스**: `UserDetailsService`

**핵심 메서드**:

```java
UserDetails loadUserByUsername(String username)
```

**동작 과정**:

1. `username` 파라미터를 `userLoginId`로 사용
2. `UsersRepository.findByUserLoginId()` 호출하여 DB 조회
3. 사용자가 없으면 `UsernameNotFoundException` 발생
4. 사용자가 있으면 `CustomUserDetails.from(Users)` 호출하여 변환
5. `CustomUserDetails` 반환

**의존성**:

- `UsersRepository`: 생성자 주입

---

### 3. CustomUserDetails (`security/CustomUserDetails.java`)

**역할**: `Users` 엔티티를 Spring Security의 `UserDetails` 인터페이스로 변환

**구현 인터페이스**: `UserDetails`

**주요 필드**:

- `userId`: 사용자 고유 ID (Long)
- `username`: 로그인 ID (`userLoginId`)
- `password`: 암호화된 비밀번호 (`userPassword`)
- `authorities`: 권한 목록 (항상 `ROLE_USER`)

**정적 팩토리 메서드**:

```java
CustomUserDetails.from(Users user)
```

- `Users` 엔티티를 받아 `CustomUserDetails` 인스턴스 생성
- 모든 사용자에게 `ROLE_USER` 권한 부여

**계정 상태 메서드** (모두 `true` 반환):

- `isAccountNonExpired()`: 계정 만료 여부
- `isAccountNonLocked()`: 계정 잠금 여부
- `isCredentialsNonExpired()`: 자격 증명 만료 여부
- `isEnabled()`: 계정 활성화 여부

---

### 4. UsersRepository (`repository/UsersRepository.java`)

**역할**: `Users` 엔티티에 대한 데이터베이스 접근 계층

**인터페이스**: `JpaRepository<Users, Long>`

**커스텀 메서드**:

```java
Optional<Users> findByUserLoginId(String userLoginId)
```

- Spring Data JPA의 메서드 네이밍 컨벤션 사용
- `userLoginId` 필드로 사용자 조회
- 결과가 없을 수 있으므로 `Optional` 반환

---

### 5. CorsConfig (`config/CorsConfig.java`)

**역할**: CORS(Cross-Origin Resource Sharing) 설정

**Bean 제공**:

- `CorsConfigurationSource`: Spring Security에서 사용하는 CORS 설정 소스

**설정 내용**:

- 모든 Origin 허용 (`*`)
- 모든 HTTP 메서드 허용
- 모든 헤더 허용
- Credentials 허용 (`allowCredentials: true`)
- Preflight 캐시 시간: 3600초

---

## 인증 흐름

### 로그인 프로세스

```
1. 사용자가 /login 페이지 접근
   ↓
2. 로그인 폼 제출 (POST /login)
   - username: userLoginId
   - password: 평문 비밀번호
   ↓
3. Spring Security Filter Chain
   - UsernamePasswordAuthenticationFilter가 요청 가로채기
   ↓
4. CustomUserDetailsService.loadUserByUsername() 호출
   - username 파라미터로 DB 조회
   ↓
5. UsersRepository.findByUserLoginId() 실행
   - DB에서 Users 엔티티 조회
   ↓
6. CustomUserDetails 생성
   - Users → CustomUserDetails 변환
   - ROLE_USER 권한 부여
   ↓
7. 비밀번호 검증
   - PasswordEncoder.matches(입력 비밀번호, DB 비밀번호)
   - BCrypt 알고리즘으로 비교
   ↓
8. 인증 성공
   - Authentication 객체 생성
   - SecurityContext에 저장
   - JSESSIONID 쿠키 생성 및 응답
   ↓
9. 리다이렉트
   - defaultSuccessUrl("/", true)에 따라 / 로 이동
```

### 인증된 요청 처리 프로세스

```
1. 인증된 사용자가 보호된 리소스 요청
   ↓
2. JSESSIONID 쿠키가 요청과 함께 전송
   ↓
3. Spring Security Filter Chain
   - SessionManagementFilter가 세션 확인
   ↓
4. SecurityContext에서 Authentication 객체 조회
   - CustomUserDetails 포함
   ↓
5. Authorization Filter
   - hasRole("USER") 체크
   - CustomUserDetails.getAuthorities()에서 ROLE_USER 확인
   ↓
6. 인가 성공 → 요청 처리 계속
   인가 실패 → 403 Forbidden 응답
```

### 로그아웃 프로세스

```
1. 사용자가 /logout 요청
   ↓
2. LogoutFilter가 요청 가로채기
   ↓
3. 세션 무효화
   - HttpSession.invalidate()
   ↓
4. JSESSIONID 쿠키 삭제
   ↓
5. SecurityContext 초기화
   ↓
6. /login?logout 으로 리다이렉트
```

---

## 설정 상세

### SecurityFilterChain 설정 분석

#### 1. CSRF 설정

```java
.csrf(AbstractHttpConfigurer::disable)
```

- **이유**: 쿠폰 발급 API의 특성상 CSRF 보호가 불필요할 수 있음
- **주의**: 프로덕션 환경에서는 필요에 따라 활성화 고려

#### 2. CORS 설정

```java
.cors(cors -> cors.configurationSource(corsConfigurationSource))
```

- `CorsConfig`에서 정의한 `CorsConfigurationSource` Bean 주입
- 모든 Origin, Method, Header 허용
- Credentials 허용으로 JSESSIONID 쿠키 전송 가능

#### 3. 세션 관리

```java
.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
)
```

- **IF_REQUIRED**: 인증이 필요할 때만 세션 생성
- 기본적으로 JSESSIONID 쿠키 사용
- 서버 메모리 또는 Redis에 세션 저장 (설정에 따라)

#### 4. URL 접근 제어

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/login", "/css/**", ...).permitAll()
    .anyRequest().hasRole("USER")
)
```

- **permitAll()**: 인증 없이 접근 가능
- **hasRole("USER")**: `ROLE_USER` 권한 필요
- 순서 중요: 구체적인 경로를 먼저, `anyRequest()`는 마지막에

#### 5. 폼 로그인

```java
.formLogin(form -> form
    .loginPage("/login")
    .loginProcessingUrl("/login")
    .defaultSuccessUrl("/", true)
)
```

- **loginPage**: 로그인 페이지 URL (GET 요청)
- **loginProcessingUrl**: 인증 처리 URL (POST 요청, form action)
- **defaultSuccessUrl**: 성공 시 리다이렉트 URL
  - 두 번째 파라미터 `true`: 항상 이 URL로 이동 (이전 요청 무시)

#### 6. 로그아웃

```java
.logout(logout -> logout
    .logoutUrl("/logout")
    .logoutSuccessUrl("/login?logout")
    .deleteCookies("JSESSIONID")
)
```

- **logoutUrl**: 로그아웃 처리 URL
- **logoutSuccessUrl**: 성공 시 리다이렉트 URL
- **deleteCookies**: 삭제할 쿠키 이름

---

## 데이터베이스 스키마

### users 테이블

```sql
CREATE TABLE users (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_login_id VARCHAR(100) NOT NULL UNIQUE,
    user_password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

### 필드 설명

| 필드명          | 타입         | 제약조건                    | 설명                            |
| --------------- | ------------ | --------------------------- | ------------------------------- |
| `user_id`       | BIGINT       | PRIMARY KEY, AUTO_INCREMENT | 사용자 고유 ID                  |
| `user_login_id` | VARCHAR(100) | NOT NULL, UNIQUE            | 로그인에 사용하는 ID (username) |
| `user_password` | VARCHAR(255) | NOT NULL                    | BCrypt로 암호화된 비밀번호      |
| `created_at`    | TIMESTAMP    | -                           | 생성 시간 (BaseTime 상속)       |
| `updated_at`    | TIMESTAMP    | -                           | 수정 시간 (BaseTime 상속)       |

### 비밀번호 저장 형식

- **알고리즘**: BCrypt
- **형식**: `$2a$10$...` (BCrypt 해시 문자열)
- **예시**: `$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy`

---

## 사용 방법

### 1. 사용자 등록 (비밀번호 암호화)

```java
@Service
public class UserService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    public Users createUser(String userLoginId, String rawPassword) {
        String encodedPassword = passwordEncoder.encode(rawPassword);

        Users user = Users.builder()
                .userLoginId(userLoginId)
                .userPassword(encodedPassword)
                .build();

        return usersRepository.save(user);
    }
}
```

### 2. 로그인 폼 (HTML)

```html
<form method="POST" action="/login">
  <input type="text" name="username" placeholder="Login ID" required />
  <input type="password" name="password" placeholder="Password" required />
  <button type="submit">Login</button>
</form>
```

**중요**:

- `name="username"`: Spring Security 기본 파라미터명 (변경 가능)
- `action="/login"`: `loginProcessingUrl`과 일치해야 함
- `method="POST"`: 필수

### 3. 현재 사용자 정보 조회

```java
@Controller
public class CouponController {

    @GetMapping("/")
    public String home(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUserId();
        String username = userDetails.getUsername();

        // 사용자 정보 활용
        return "home";
    }
}
```

또는 `SecurityContextHolder` 사용:

```java
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
```

### 4. 권한 체크 (메서드 레벨)

```java
@PreAuthorize("hasRole('USER')")
@GetMapping("/coupons")
public ResponseEntity<List<Coupon>> getCoupons() {
    // ROLE_USER 권한이 있는 사용자만 접근 가능
}
```

---

## 보안 고려사항

### ✅ 구현된 보안 기능

- 비밀번호 BCrypt 암호화
- 세션 기반 인증 (JSESSIONID)
- 역할 기반 접근 제어 (RBAC)
- CORS 설정

### ⚠️ 추가 고려사항

1. **CSRF 보호**: 현재 비활성화 상태. 필요 시 활성화 고려
2. **세션 타임아웃**: 기본값 사용 중. 필요 시 설정 추가
3. **비밀번호 정책**: 최소 길이, 복잡도 등 검증 로직 추가 고려
4. **계정 잠금**: 로그인 실패 횟수 제한 기능 고려
5. **HTTPS**: 프로덕션 환경에서는 HTTPS 사용 권장

---

## 트러블슈팅

### 문제: 로그인 후에도 403 Forbidden 발생

**원인**: `hasRole("USER")`는 내부적으로 `ROLE_USER`를 찾지만, `CustomUserDetails`에서 `ROLE_USER`를 제대로 반환하지 않는 경우

**해결**: `CustomUserDetails.from()` 메서드에서 `SimpleGrantedAuthority("ROLE_USER")`가 제대로 생성되는지 확인

### 문제: JSESSIONID 쿠키가 생성되지 않음

**원인**: CORS 설정에서 `allowCredentials: true`이지만, 프론트엔드에서 `withCredentials: true`를 설정하지 않은 경우

**해결**:

- 프론트엔드: `fetch(url, { credentials: 'include' })` 또는 `axios.defaults.withCredentials = true`
- CORS 설정: `allowedOrigins`에 `"*"` 대신 구체적인 Origin 지정

### 문제: 로그인 성공 후 이전 페이지로 이동하지 않음

**원인**: `defaultSuccessUrl("/", true)`의 두 번째 파라미터가 `true`로 설정되어 항상 `/`로 이동

**해결**: `defaultSuccessUrl("/")`로 변경하면 이전 요청한 페이지로 리다이렉트

---

## 참고 자료

- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [BCrypt 알고리즘](https://en.wikipedia.org/wiki/Bcrypt)
