# Coupon Service – 구현 방식 및 작동 원리

쿠폰 발급 서비스의 구현 방식과 Redis 기반 선착순·중복 방지 작동 원리를 정리한 문서입니다.

---

## 1. 개요

| 구분     | 설명                                                                                         |
| -------- | -------------------------------------------------------------------------------------------- |
| **목적** | 동시 다수 요청 환경에서 **선착순**으로 쿠폰을 발급하고, **중복 발급**과 **초과 발급**을 방지 |
| **구성** | `CouponService`(비즈니스·DB), `CouponRedisService`(Redis·원자 연산)                          |
| **원칙** | Redis에서 선착순·중복 판단 → 성공 시에만 DB에 발급 이력 저장                                 |

---

## 2. 구현 방식

### 2.1 역할 분리

- **CouponService**
  - API 진입점, 유효성 검사(쿠폰/유저 존재, 발급 기간)
  - Redis 재고 초기화·발급 시도 호출
  - Redis 발급 성공 시에만 `UserCoupon` 엔티티 생성 후 DB 저장
- **CouponRedisService**
  - Redis 키 설계 및 Lua 스크립트 실행
  - 재고 초기값 저장(`initializeStock`), 재고 보조 초기화(`ensureStockIfAbsent`), 발급 시도(`tryIssue`) 담당

### 2.2 Redis 키 설계

| 키 패턴                          | 타입   | 용도                                                            |
| -------------------------------- | ------ | --------------------------------------------------------------- |
| `coupon:{couponId}:stock`        | String | 남은 재고 수량. 초기값 = DB `coupon_total_count`, `DECR`로 차감 |
| `coupon:{couponId}:issued_users` | Set    | 해당 쿠폰을 이미 받은 `userId` 목록. `SADD`/`SISMEMBER` 사용    |

- **초기값 저장**: 이벤트/쿠폰이 **열릴 때** DB의 `coupon_total_count`(초기 개수)를 Redis에 미리 세팅한다.
  - `CouponRedisService.initializeStock(couponId, totalCount)` — 단일 쿠폰 초기 재고 세팅
  - `EventService.initializeCouponStocksForEvent(eventId)` — 해당 이벤트의 모든 쿠폰 초기 재고 세팅
  - API: `POST /events/{eventId}/initialize-coupons` 호출 시 해당 이벤트 쿠폰들의 초기 개수가 Redis에 저장된다.
- **보조 초기화**: 발급 요청 시점에 Redis에 재고 키가 없으면 `ensureStockIfAbsent`로 NX 세팅(이미 있으면 덮어쓰지 않음).
- 발급 성공 시마다 `issued_users` Set에 `userId` 추가.

### 2.3 Lua 스크립트로 원자 처리

여러 Redis 명령(중복 체크 → 재고 차감 → 발급 등록)을 **한 번에** 실행해 경쟁 조건을 제거합니다.

**처리 순서**

1. `SISMEMBER(issued_users, userId)` → 이미 있으면 **-1** 반환(이미 발급됨).
2. `DECR(stock)` → 재고 1 감소.
3. 결과가 **음수**면 `INCR(stock)`으로 롤백 후 **0** 반환(재고 소진).
4. 그렇지 않으면 `SADD(issued_users, userId)` 후 **1** 반환(발급 성공).

**반환값**

| 반환값 | 의미                              |
| ------ | --------------------------------- |
| `1`    | 발급 성공                         |
| `0`    | 재고 소진                         |
| `-1`   | 해당 유저는 이미 해당 쿠폰 발급됨 |

### 2.4 Redis 초기값(기초값) 저장

열린 쿠폰의 **초기 개수** 등 기초값은 이벤트/쿠폰이 열릴 때 Redis에 저장하는 것을 권장한다.

| 시점            | 방법                                                                                                                                 |
| --------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| 이벤트 오픈 시  | `POST /events/{eventId}/initialize-coupons` 호출 → 해당 이벤트의 모든 쿠폰 `coupon_total_count` 를 Redis `coupon:{id}:stock` 에 세팅 |
| 서비스/배치에서 | `EventService.initializeCouponStocksForEvent(eventId)` 또는 `CouponRedisService.initializeStock(couponId, totalCount)` 호출          |

- **initializeStock**: Redis에 해당 쿠폰의 초기 재고를 **설정**한다(SET). 이미 값이 있어도 덮어쓴다.
- **ensureStockIfAbsent**: 키가 **없을 때만** DB 기준 수량으로 세팅(NX). 발급 API에서 Redis 키가 없을 때의 보조 초기화용.

---

## 3. 작동 원리 (발급 API 흐름)

```
[클라이언트] → issueCoupon(userId, couponId)
       │
       ▼
┌──────────────────────────────────────────────────────────────┐
│ 1. DB 검증                                                     │
│    - 쿠폰 존재 여부 → CouponNotFoundException                  │
│    - 유저 존재 여부 → UserNotFoundException                    │
│    - 발급 기간(couponApplyStartDatetime ~ couponApplyEndDatetime) │
│      → 기간 외 요청 시 CouponExpiredException                  │
└──────────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────┐
│ 2. Redis 재고 준비 (CouponRedisService)                       │
│    - ensureStockIfAbsent(couponId, couponTotalCount)          │
│    - coupon:{couponId}:stock 가 없을 때만 NX로 totalCount 세팅  │
│    - (권장) 이벤트 오픈 시 POST /events/{id}/initialize-coupons│
│      로 미리 초기 개수(기초값)를 Redis에 저장해 두면 유리함     │
└──────────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────┐
│ 3. Redis 발급 시도 (Lua 스크립트 원자 실행)                    │
│    - tryIssue(couponId, userId)                                │
│    - 중복 체크 → 재고 차감 → (실패 시 롤백) / (성공 시 SADD)   │
│    - 반환: 1(성공) / 0(재고 소진) / -1(이미 발급)              │
└──────────────────────────────────────────────────────────────┘
       │
       ├─ -1 → DuplicateCouponException
       ├─  0 → CouponExhaustedException
       │
       ▼ 1 (성공)
┌──────────────────────────────────────────────────────────────┐
│ 4. DB 저장                                                     │
│    - UserCoupon(user, coupon, CouponStatus.NOT_USE) 생성       │
│    - userCouponRepository.save(userCoupon)                     │
└──────────────────────────────────────────────────────────────┘
       │
       ▼
   [정상 응답]
```

- **선착순**: Lua 안에서 `DECR`이 한 번만 실행되므로, 동시 요청이 많아도 재고 수만큼만 1이 반환됩니다.
- **중복 방지**: `SISMEMBER`로 먼저 확인하므로, 같은 유저가 같은 쿠폰을 두 번 받지 못합니다.
- **초과 발급 방지**: 재고가 0 이하가 되면 DECR 결과가 음수이므로 롤백 후 0을 반환해 DB 저장이 이루어지지 않습니다.

---

## 4. 예외 및 에러 처리

| 예외                       | 발생 시점                                                |
| -------------------------- | -------------------------------------------------------- |
| `CouponNotFoundException`  | 쿠폰 ID에 해당하는 쿠폰이 DB에 없음                      |
| `UserNotFoundException`    | 유저 ID에 해당하는 유저가 DB에 없음                      |
| `CouponExpiredException`   | 현재 시각이 쿠폰 발급 가능 기간 밖                       |
| `DuplicateCouponException` | Redis Lua 반환 -1 (이미 해당 쿠폰 발급됨)                |
| `CouponExhaustedException` | Redis Lua 반환 0 (재고 소진)                             |
| `RedisConnectionException` | Redis 연결/명령 실행 실패 (재고 초기화 또는 tryIssue 중) |

---

## 5. 정리

- **Redis**: 선착순·중복·재고 제어를 Lua로 원자 처리해, 동시 요청에서도 수량과 1인 1장이 보장됩니다.
- **DB**: Redis에서 성공(반환 1)인 경우에만 `UserCoupon`을 저장해, 최종 발급 이력만 보관합니다.
- **구현**: `CouponService`는 흐름과 DB, `CouponRedisService`는 Redis 키와 Lua만 담당하도록 역할이 나뉘어 있습니다.
