Live Coupon (선착순 쿠폰 발급)
==============================

## 프로젝트 개요
- 다수 사용자가 동시에 접속하는 환경에서 쿠폰을 선착순으로 발급.
- Redis를 활용해 재고 차감/중복 발급 방지 등을 원자적으로 처리하고, DB에는 최종 결과만 적재.
- React(프론트) ↔ Spring Boot(백엔드) ↔ Redis ↔ RDBMS 구조.

## 기술 스택
- Backend: Spring Boot, Java 24, Maven
- Infra: Redis, RDBMS(프로젝트 설정에 따라), K6 부하 테스트
- ETC: JUnit5 기반 테스트, Gradle 아님(Maven 사용)

## 주요 기능 요약
- 쿠폰 재고 관리: Redis `DECR` 기반 원자적 차감 (`coupon:{couponId}:stock`).
- 중복 발급 방지: Redis Set (`coupon:{couponId}:issued_users`)로 사용자 발급 여부 확인.
- 이벤트 활성 상태: `event:{eventId}:active` 키로 시작/종료 제어.
- DB 저장: Redis 처리 성공 시 발급 이력만 DB에 기록.
- 보완 전략: DB 저장 실패 시 로깅 및 재처리 고려.

## Redis 처리 흐름 (발급)
1) 이벤트 활성 여부 확인 (`event:{eventId}:active`).
2) 중복 발급 여부 확인 (`SISMEMBER coupon:{couponId}:issued_users userId`).
3) 재고 차감 (`DECR coupon:{couponId}:stock`).
4) 재고가 0 이상이면 발급 성공 → 사용자 Set에 추가 (`SADD`).
5) 성공 시 DB에 발급 이력 저장, 결과 반환.
6) Redis Lua 스크립트로 2~4 단계를 원자적으로 처리해 과발급/중복을 방지.

## k6 부하 테스트 사용법
k6 스크립트 위치: `k6/coupon.js`  
테스트용 사용자 데이터: `k6/users.json`

### 사전 준비
1) Redis와 백엔드 서버를 실행 (필요 시 DB도 준비).
2) `k6` CLI 설치 (macOS 예: `brew install k6`).

### 기본 실행 예시
```bash
# 프로젝트 루트 기준
k6 run k6/coupon.js
```
- `coupon.js`에서 목표 VU, duration, 대상 URL, 헤더 등을 설정.
- `users.json`에 다수의 사용자 ID가 정의되어 있어 동시 발급 시나리오를 시뮬레이션.

### 스크립트 개요 (`k6/coupon.js`)
- 다수 가상의 사용자로 로그인 → 쿠폰 발급 API 호출 → 응답 상태/시간을 측정.
- 성공/실패 사유(재고 부족, 중복 발급 차단 등)를 확인하여 Redis 기반 로직이 의도대로 동작하는지 검증.

## 실행 및 개발 메모
- Maven: `./mvnw clean test` 또는 `./mvnw spring-boot:run`
- 애플리케이션 설정: `src/main/resources/application.properties`에서 Redis/DB 접속 정보 확인.
- 프론트엔드: React 앱(별도 경로)에서 백엔드 API로만 통신, Redis 직접 접근 없음.

## 파일 구조 메모
- `src/main/java/com/project/coupon/controller` : API 엔드포인트
- `src/main/java/com/project/coupon/service`    : 쿠폰 발급 비즈니스 로직
- `src/main/java/com/project/coupon/repository` : JPA Repository
- `src/main/java/com/project/coupon/dto`        : 요청/응답 DTO
- `k6/`                                         : 부하 테스트 스크립트 및 사용자 데이터

## 참고 문서
- `.cursor/rules/project.mdc` : 전반 요구사항/처리 방식
- `.cursor/rules/redis.mdc`   : Redis 키 설계 및 Lua 처리 개요