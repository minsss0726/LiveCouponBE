# 예외 처리 시스템 (Exception Handling System)

## 목차

1. [개요](#개요)
2. [시스템 구조](#시스템-구조)
3. [핵심 컴포넌트](#핵심-컴포넌트)
4. [동작 원리](#동작-원리)
5. [예외 클래스 상세](#예외-클래스-상세)
6. [사용 예시](#사용-예시)
7. [HTTP 상태 코드 매핑](#http-상태-코드-매핑)
8. [설계 원칙](#설계-원칙)

---

## 개요

이 예외 처리 시스템은 Live Coupon 프로젝트의 비즈니스 로직에서 발생하는 모든 예외를 일관된 형식으로 처리하고, 클라이언트에게 명확한 오류 정보를 제공합니다.

### 주요 특징

- **일관된 오류 응답 형식**: 모든 예외를 `ErrorResponse` 형식으로 통일
- **명확한 오류 코드**: 각 예외 타입별 고유한 오류 코드 제공
- **자동 HTTP 상태 코드 매핑**: 예외 타입에 따라 적절한 HTTP 상태 코드 자동 결정
- **전역 예외 처리**: `@RestControllerAdvice`를 통한 중앙 집중식 예외 처리
- **불변 객체 설계**: 데이터 무결성 보장

---

## 시스템 구조

```
exception/
├── BaseException.java              # 모든 커스텀 예외의 기본 클래스 (abstract)
├── ErrorResponse.java              # 오류 응답 DTO (final, immutable)
├── GlobalExceptionHandler.java     # 전역 예외 핸들러 (final)
│
├── CouponExhaustedException.java      # 쿠폰 재고 부족
├── DuplicateCouponException.java      # 중복 쿠폰 발급
├── CouponExpiredException.java         # 쿠폰 발급 기간 만료
├── CouponNotFoundException.java       # 쿠폰 없음
├── UserNotFoundException.java         # 사용자 없음
├── InvalidRequestException.java        # 잘못된 요청
└── RedisConnectionException.java       # Redis 연결 오류
```

### 클래스 계층 구조

```
RuntimeException
    └── BaseException (abstract)
            ├── CouponExhaustedException (final)
            ├── DuplicateCouponException (final)
            ├── CouponExpiredException (final)
            ├── CouponNotFoundException (final)
            ├── UserNotFoundException (final)
            ├── InvalidRequestException (final)
            └── RedisConnectionException (final)
```

---

## 핵심 컴포넌트

### 1. BaseException (추상 기본 클래스)

**역할**: 모든 커스텀 예외의 공통 기능 제공

**특징**:

- `abstract` 클래스로 직접 인스턴스화 불가
- `errorCode`와 `message`를 포함한 공통 구조 제공
- `RuntimeException` 상속 (unchecked exception)

**주요 메서드**:

```java
protected BaseException(String errorCode, String message)
protected BaseException(String errorCode, String message, Throwable cause)
public String getErrorCode()
```

### 2. ErrorResponse (불변 DTO)

**역할**: 클라이언트에게 전달되는 오류 응답 형식

**특징**:

- `final` 클래스 + `final` 필드로 불변 객체
- 정적 팩토리 메서드(`from`)로 생성
- JSON 직렬화 지원 (`@JsonFormat`)

**필드**:

- `errorCode`: 오류 코드 (예: "COUPON_EXHAUSTED")
- `message`: 오류 메시지
- `timestamp`: 오류 발생 시각
- `path`: 요청 경로

### 3. GlobalExceptionHandler (전역 예외 핸들러)

**역할**: 애플리케이션 전역에서 발생하는 예외를 일관된 형식으로 처리

**특징**:

- `@RestControllerAdvice`로 모든 컨트롤러의 예외 처리
- 예외 타입별 핸들러 메서드 제공
- 자동 HTTP 상태 코드 매핑

**처리하는 예외 타입**:

1. `BaseException` 및 하위 예외
2. `MethodArgumentNotValidException` (요청 파라미터 검증 실패)
3. `BindException` (바인딩 오류)
4. `IllegalArgumentException` (잘못된 인자)
5. `Exception` (기타 모든 예외)

---

## 동작 원리

### 예외 처리 흐름도

```
[Controller/Service Layer]
        │
        │ 예외 발생
        ▼
[GlobalExceptionHandler]
        │
        │ @ExceptionHandler로 예외 캐치
        ▼
[예외 타입별 핸들러 메서드]
        │
        │ ErrorResponse 생성
        ▼
[HTTP 상태 코드 결정]
        │
        │ ResponseEntity 생성
        ▼
[클라이언트에게 응답]
```

### 상세 동작 과정

#### 1. 예외 발생 단계

```java
// Service Layer에서 예외 발생
if (remainingCount < 0) {
    throw new CouponExhaustedException(couponId);
}
```

#### 2. 예외 캐치 단계

`GlobalExceptionHandler`의 `@ExceptionHandler(BaseException.class)`가 예외를 캐치합니다.

```java
@ExceptionHandler(BaseException.class)
public ResponseEntity<ErrorResponse> handleBaseException(
        final BaseException exception,
        final WebRequest request) {
    // 예외 처리 로직
}
```

#### 3. ErrorResponse 생성 단계

예외 정보를 `ErrorResponse`로 변환합니다.

```java
final ErrorResponse errorResponse = ErrorResponse.from(exception, path);
```

#### 4. HTTP 상태 코드 결정 단계

예외의 `errorCode`를 기반으로 적절한 HTTP 상태 코드를 결정합니다.

```java
private HttpStatus determineHttpStatus(final BaseException exception) {
    return switch (exception.getErrorCode()) {
        case "COUPON_EXHAUSTED" -> HttpStatus.CONFLICT;
        case "COUPON_NOT_FOUND" -> HttpStatus.NOT_FOUND;
        // ...
    };
}
```

#### 5. 응답 반환 단계

`ResponseEntity`로 래핑하여 클라이언트에게 반환합니다.

```java
return ResponseEntity.status(status).body(errorResponse);
```

---

## 예외 클래스 상세

### 비즈니스 예외

#### 1. CouponExhaustedException

**발생 시점**: Redis에서 쿠폰 수량을 차감한 결과가 음수일 때

**오류 코드**: `COUPON_EXHAUSTED`

**HTTP 상태 코드**: `409 CONFLICT`

**사용 예시**:

```java
// Redis DECR 결과가 음수인 경우
Long remaining = redisTemplate.opsForValue().decrement("coupon:count:" + couponId);
if (remaining < 0) {
    throw new CouponExhaustedException(couponId);
}
```

#### 2. DuplicateCouponException

**발생 시점**: 사용자가 이미 동일한 쿠폰을 발급받았을 때

**오류 코드**: `DUPLICATE_COUPON`

**HTTP 상태 코드**: `409 CONFLICT`

**사용 예시**:

```java
// Redis SETNX 실패 시 (이미 존재하는 경우)
Boolean isNew = redisTemplate.opsForSet().add("coupon:issued:" + couponId, userId);
if (!isNew) {
    throw new DuplicateCouponException(userId, couponId);
}
```

#### 3. CouponExpiredException

**발생 시점**: 쿠폰 발급 기간이 만료되었거나 아직 시작되지 않았을 때

**오류 코드**: `COUPON_EXPIRED`

**HTTP 상태 코드**: `409 CONFLICT`

**사용 예시**:

```java
LocalDateTime now = LocalDateTime.now();
if (now.isBefore(coupon.getCouponApplyStartDatetime())) {
    throw new CouponExpiredException(couponId, coupon.getCouponApplyStartDatetime());
}
if (now.isAfter(coupon.getCouponApplyEndDatetime())) {
    throw new CouponExpiredException(couponId,
        coupon.getCouponApplyStartDatetime(),
        coupon.getCouponApplyEndDatetime());
}
```

#### 4. CouponNotFoundException

**발생 시점**: 요청한 쿠폰 ID에 해당하는 쿠폰이 존재하지 않을 때

**오류 코드**: `COUPON_NOT_FOUND`

**HTTP 상태 코드**: `404 NOT_FOUND`

**사용 예시**:

```java
Coupons coupon = couponRepository.findById(couponId)
    .orElseThrow(() -> new CouponNotFoundException(couponId));
```

#### 5. UserNotFoundException

**발생 시점**: 요청한 사용자 ID에 해당하는 사용자가 존재하지 않을 때

**오류 코드**: `USER_NOT_FOUND`

**HTTP 상태 코드**: `404 NOT_FOUND`

**사용 예시**:

```java
Users user = userRepository.findById(userId)
    .orElseThrow(() -> new UserNotFoundException(userId));
```

### 시스템 예외

#### 6. InvalidRequestException

**발생 시점**: 요청 파라미터가 유효하지 않거나 비즈니스 규칙에 위배될 때

**오류 코드**: `INVALID_REQUEST`

**HTTP 상태 코드**: `400 BAD_REQUEST`

**사용 예시**:

```java
if (couponId == null || couponId <= 0) {
    throw new InvalidRequestException("couponId", couponId);
}
```

#### 7. RedisConnectionException

**발생 시점**: Redis 서버와의 연결이 실패하거나 작업 중 오류가 발생했을 때

**오류 코드**: `REDIS_CONNECTION_ERROR`

**HTTP 상태 코드**: `503 SERVICE_UNAVAILABLE`

**사용 예시**:

```java
try {
    redisTemplate.opsForValue().set(key, value);
} catch (RedisConnectionFailureException e) {
    throw new RedisConnectionException("쿠폰 수량 설정 실패", e);
}
```

---

## 사용 예시

### Service Layer에서 예외 발생

```java
@Service
public class CouponService {

    public void issueCoupon(Long userId, Long couponId) {
        // 1. 쿠폰 존재 확인
        Coupons coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new CouponNotFoundException(couponId));

        // 2. 사용자 존재 확인
        Users user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        // 3. 발급 기간 확인
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getCouponApplyStartDatetime()) ||
            now.isAfter(coupon.getCouponApplyEndDatetime())) {
            throw new CouponExpiredException(couponId,
                coupon.getCouponApplyStartDatetime(),
                coupon.getCouponApplyEndDatetime());
        }

        // 4. 중복 발급 확인
        Boolean isNew = redisTemplate.opsForSet()
            .add("coupon:issued:" + couponId, userId);
        if (!isNew) {
            throw new DuplicateCouponException(userId, couponId);
        }

        // 5. 재고 확인 및 차감
        Long remaining = redisTemplate.opsForValue()
            .decrement("coupon:count:" + couponId);
        if (remaining < 0) {
            throw new CouponExhaustedException(couponId);
        }

        // 6. 발급 처리
        // ...
    }
}
```

### Controller Layer (예외 처리 불필요)

```java
@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    @Autowired
    private CouponService couponService;

    @PostMapping("/{couponId}/issue")
    public ResponseEntity<CouponIssueResponse> issueCoupon(
            @PathVariable Long couponId,
            @RequestHeader("X-User-Id") Long userId) {

        // 예외는 GlobalExceptionHandler가 자동 처리
        couponService.issueCoupon(userId, couponId);

        return ResponseEntity.ok(new CouponIssueResponse(couponId, userId));
    }
}
```

### 클라이언트 응답 예시

#### 성공 응답

```json
{
  "couponId": 1,
  "userId": 100
}
```

#### 오류 응답 (쿠폰 재고 부족)

```json
{
  "errorCode": "COUPON_EXHAUSTED",
  "message": "쿠폰 재고가 부족합니다. couponId: 1",
  "timestamp": "2026-01-28 14:30:00",
  "path": "/api/coupons/1/issue"
}
```

#### 오류 응답 (중복 발급)

```json
{
  "errorCode": "DUPLICATE_COUPON",
  "message": "이미 발급받은 쿠폰입니다. userId: 100, couponId: 1",
  "timestamp": "2026-01-28 14:30:00",
  "path": "/api/coupons/1/issue"
}
```

#### 오류 응답 (요청 파라미터 검증 실패)

```json
{
  "errorCode": "INVALID_REQUEST",
  "message": "요청 파라미터가 유효하지 않습니다. [couponId: must be greater than 0]",
  "timestamp": "2026-01-28 14:30:00",
  "path": "/api/coupons/-1/issue"
}
```

---

## HTTP 상태 코드 매핑

| 예외 클래스                | 오류 코드                | HTTP 상태 코드              | 의미                         |
| -------------------------- | ------------------------ | --------------------------- | ---------------------------- |
| `CouponExhaustedException` | `COUPON_EXHAUSTED`       | `409 CONFLICT`              | 리소스 상태 충돌 (재고 부족) |
| `DuplicateCouponException` | `DUPLICATE_COUPON`       | `409 CONFLICT`              | 리소스 상태 충돌 (중복)      |
| `CouponExpiredException`   | `COUPON_EXPIRED`         | `409 CONFLICT`              | 리소스 상태 충돌 (만료)      |
| `CouponNotFoundException`  | `COUPON_NOT_FOUND`       | `404 NOT_FOUND`             | 리소스를 찾을 수 없음        |
| `UserNotFoundException`    | `USER_NOT_FOUND`         | `404 NOT_FOUND`             | 리소스를 찾을 수 없음        |
| `InvalidRequestException`  | `INVALID_REQUEST`        | `400 BAD_REQUEST`           | 잘못된 요청                  |
| `RedisConnectionException` | `REDIS_CONNECTION_ERROR` | `503 SERVICE_UNAVAILABLE`   | 서비스 일시 중단             |
| 기타 예외                  | `INTERNAL_SERVER_ERROR`  | `500 INTERNAL_SERVER_ERROR` | 서버 내부 오류               |

---

## 설계 원칙

### 1. Effective Java 가이드라인 준수

- **Item 4**: "Enforce noninstantiability with a private constructor"
  - `BaseException`을 `abstract`로 설계하여 직접 인스턴스화 방지
- **Item 17**: "Design and document for inheritance or else prohibit it"
  - 모든 예외 클래스를 `final`로 설계하여 상속 방지
- **Item 10**: "Obey the general contract when overriding equals"
  - 불변 객체 설계로 동등성 비교 안정성 보장

### 2. 불변성 (Immutability)

- **ErrorResponse**: `final` 클래스 + `final` 필드로 불변 객체
- **예외 클래스**: 생성 시점에 모든 정보가 고정되어 변경 불가

### 3. 단일 책임 원칙 (SRP)

- 각 예외 클래스는 하나의 비즈니스 상황만 표현
- `GlobalExceptionHandler`는 예외 처리만 담당

### 4. 개방-폐쇄 원칙 (OCP)

- 새로운 예외 타입 추가 시 기존 코드 수정 없이 확장 가능
- `BaseException`을 상속하여 새로운 예외 추가 용이

### 5. Data-Oriented Programming (DOP)

- **불변 데이터**: `ErrorResponse`는 불변 객체로 설계
- **순수 함수**: `ErrorResponse.from()`은 부작용 없는 순수 함수

### 6. 함수형 프로그래밍 (FP)

- **불변 객체 사용**: 상태 변경을 최소화
- **부작용 최소화**: 예외 처리 로직이 예측 가능

---

## 확장 가이드

### 새로운 예외 클래스 추가하기

1. `BaseException`을 상속하는 `final` 클래스 생성

```java
public final class NewBusinessException extends BaseException {
    private static final String ERROR_CODE = "NEW_ERROR_CODE";

    public NewBusinessException(final Long id) {
        super(ERROR_CODE, String.format("새로운 오류 발생. id: %d", id));
    }
}
```

2. `GlobalExceptionHandler.determineHttpStatus()`에 HTTP 상태 코드 매핑 추가

```java
case "NEW_ERROR_CODE" -> HttpStatus.BAD_REQUEST;
```

### 새로운 예외 핸들러 추가하기

`GlobalExceptionHandler`에 새로운 핸들러 메서드 추가:

```java
@ExceptionHandler(CustomException.class)
public ResponseEntity<ErrorResponse> handleCustomException(
        final CustomException exception,
        final WebRequest request) {
    // 처리 로직
}
```

---

## 주의사항

1. **예외는 예외적인 상황에만 사용**: 정상적인 제어 흐름에는 사용하지 않음
2. **예외 메시지는 명확하게**: 클라이언트가 이해할 수 있는 메시지 작성
3. **원인 예외 포함**: 시스템 예외는 `cause`를 포함하여 디버깅 용이성 확보
4. **로깅**: `GlobalExceptionHandler`에서 적절한 레벨로 로깅 수행

---

## 참고 자료

- [Effective Java 3rd Edition](https://www.oreilly.com/library/view/effective-java/9780134686097/)
- [Spring Boot Exception Handling](https://spring.io/guides/gs/rest-service/)
- 프로젝트 규칙: `.cursor/rules/java.mdc`
- 프로젝트 요구사항: `.cursor/rules/project.mdc`
