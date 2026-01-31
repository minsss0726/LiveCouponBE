package com.project.coupon.service;

import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.project.coupon.exception.RedisConnectionException;

import lombok.RequiredArgsConstructor;

/**
 * 쿠폰 발급의 선착순·중복 방지를 위한 Redis 연산 서비스.
 * Lua 스크립트로 중복 체크·재고 차감·발급 등록을 원자적으로 수행한다.
 * redis.mdc: event:active, rate:user/ip, coupon:detail, coupon:active 지원.
 */
@Service
@RequiredArgsConstructor
public class CouponRedisService {

    private static final String STOCK_KEY_PREFIX = "coupon:";
    private static final String STOCK_KEY_SUFFIX = ":stock";
    private static final String ISSUED_USERS_KEY_SUFFIX = ":issued_users";
    private static final String EVENT_ACTIVE_KEY_PREFIX = "event:";
    private static final String EVENT_ACTIVE_SUFFIX = ":active";
    private static final String RATE_USER_PREFIX = "rate:user:";
    private static final String RATE_IP_PREFIX = "rate:ip:";
    private static final String COUPON_DETAIL_KEY_PREFIX = "coupon:detail:";
    private static final String COUPON_ACTIVE_KEY_PREFIX = "coupon:active:";

    /** Rate limit: 윈도우(초), redis.mdc 3.4 */
    private static final long RATE_LIMIT_WINDOW_SECONDS = 60L;
    /** Rate limit: 윈도우 내 최대 요청 수 */
    private static final int RATE_LIMIT_MAX_REQUESTS = 10;

    /**
     * Lua script: 중복 체크 → 재고 DECR → 실패 시 롤백, 성공 시 SADD.
     * KEYS[1]: issued_users key, KEYS[2]: stock key, ARGV[1]: userId
     * Return: 1 = 발급 성공, 0 = 재고 소진, -1 = 이미 발급됨
     */
    private static final String COUPON_ISSUE_SCRIPT =
        "if redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 1 then "
        + "  return -1 "
        + "end "
        + "local remaining = redis.call('DECR', KEYS[2]) "
        + "if remaining < 0 then "
        + "  redis.call('INCR', KEYS[2]) "
        + "  return 0 "
        + "end "
        + "redis.call('SADD', KEYS[1], ARGV[1]) "
        + "return 1";

    /** Rate limit: INCR + 최초 시 EXPIRE. KEYS[1]: rate key, ARGV[1]: TTL(초) */
    private static final String RATE_INCR_SCRIPT =
        "local v = redis.call('INCR', KEYS[1]) "
        + "if v == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end "
        + "return v";

    private final RedisTemplate<String, String> stringRedisTemplate;

    /**
     * 쿠폰이 열릴 때(이벤트 시작 등) Redis에 초기 재고를 저장한다.
     * DB의 coupon_total_count(초기 개수)를 그대로 세팅할 때 사용.
     *
     * @param couponId   쿠폰 ID
     * @param totalCount 초기 재고 수 (coupon_total_count)
     */
    public void initializeStock(final Long couponId, final int totalCount) {
        String key = stockKey(couponId);
        try {
            stringRedisTemplate.opsForValue().set(key, String.valueOf(totalCount));
        } catch (Exception e) {
            throw new RedisConnectionException("재고 초기값 저장 실패: " + key, e);
        }
    }

    /**
     * 쿠폰 재고 키가 없을 때만 DB 기준 수량으로 초기화한다. (NX)
     * 이미 Redis에 초기값이 세팅된 경우 덮어쓰지 않는다.
     */
    public void ensureStockIfAbsent(final Long couponId, final int totalCount) {
        String key = stockKey(couponId);
        try {
            stringRedisTemplate.opsForValue().setIfAbsent(key, String.valueOf(totalCount));
        } catch (Exception e) {
            throw new RedisConnectionException("재고 키 초기화 실패: " + key, e);
        }
    }

    /**
     * Lua 스크립트로 선착순·중복 방지 쿠폰 발급을 시도한다.
     *
     * @param couponId 쿠폰 ID
     * @param userId   유저 ID
     * @return 1 발급 성공, 0 재고 소진, -1 이미 발급됨
     */
    public int tryIssue(final Long couponId, final Long userId) {
        String issuedKey = issuedUsersKey(couponId);
        String stockKey = stockKey(couponId);
        String userIdStr = String.valueOf(userId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(COUPON_ISSUE_SCRIPT, Long.class);
        try {
            Long result = stringRedisTemplate.execute(
                script,
                java.util.List.of(issuedKey, stockKey),
                userIdStr
            );
            return result != null ? result.intValue() : 0;
        } catch (Exception e) {
            throw new RedisConnectionException("쿠폰 발급 Redis 처리 실패. couponId: " + couponId, e);
        }
    }

    // ----- event:{eventId}:active (redis.mdc 3.3) -----

    /**
     * 이벤트 활성 상태를 Redis에 설정한다. TTL 만료 시 자동 제거.
     *
     * @param eventId     이벤트 ID
     * @param ttlSeconds  유효 시간(초), 이벤트 종료 시각까지 권장
     */
    public void setEventActive(final Long eventId, final long ttlSeconds) {
        String key = eventActiveKey(eventId);
        try {
            stringRedisTemplate.opsForValue().set(key, "true", java.time.Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            throw new RedisConnectionException("이벤트 활성 상태 저장 실패: " + key, e);
        }
    }

    /**
     * Redis에 이벤트가 활성인지 조회한다.
     *
     * @param eventId 이벤트 ID
     * @return 활성이면 true
     */
    public boolean isEventActive(final Long eventId) {
        String key = eventActiveKey(eventId);
        try {
            Boolean has = stringRedisTemplate.hasKey(key);
            return Boolean.TRUE.equals(has);
        } catch (Exception e) {
            throw new RedisConnectionException("이벤트 활성 조회 실패: " + key, e);
        }
    }

    // ----- rate:user:{userId}, rate:ip:{ipAddress} (redis.mdc 3.4) -----

    /**
     * Rate limit 확인 후 카운트를 증가시킨다. 허용 시 true, 초과 시 false.
     *
     * @param userId   유저 ID
     * @param clientIp 클라이언트 IP (null/blank면 유저만 체크)
     * @return 허용 여부
     */
    public boolean checkAndIncrementRate(final Long userId, final String clientIp) {
        try {
            String userKey = RATE_USER_PREFIX + userId;
            Long userCount = incrWithTtl(userKey, RATE_LIMIT_WINDOW_SECONDS);
            if (userCount > RATE_LIMIT_MAX_REQUESTS) {
                return false;
            }
            if (clientIp != null && !clientIp.isBlank()) {
                String ipKey = RATE_IP_PREFIX + clientIp;
                Long ipCount = incrWithTtl(ipKey, RATE_LIMIT_WINDOW_SECONDS);
                if (ipCount > RATE_LIMIT_MAX_REQUESTS) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            throw new RedisConnectionException("Rate limit 처리 실패", e);
        }
    }

    private Long incrWithTtl(final String key, final long ttlSeconds) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RATE_INCR_SCRIPT, Long.class);
        Long v = stringRedisTemplate.execute(script, java.util.List.of(key), String.valueOf(ttlSeconds));
        return v != null ? v : 0L;
    }

    // ----- Redis 성공 후 DB 저장 실패 시 롤백 (redis.mdc 6) -----

    /**
     * Lua 발급 성공 후 DB 저장 실패 시 Redis를 원상 복구한다.
     * INCR stock, SREM issued_users.
     *
     * @param couponId 쿠폰 ID
     * @param userId   유저 ID
     */
    public void rollbackIssue(final Long couponId, final Long userId) {
        String stockKey = stockKey(couponId);
        String issuedKey = issuedUsersKey(couponId);
        String userIdStr = String.valueOf(userId);
        try {
            stringRedisTemplate.opsForValue().increment(stockKey);
            stringRedisTemplate.opsForSet().remove(issuedKey, userIdStr);
        } catch (Exception e) {
            throw new RedisConnectionException("쿠폰 발급 롤백 실패. couponId: " + couponId, e);
        }
    }

    // ----- coupon:detail:{couponId} (project.mdc 3.3) -----

    /**
     * 쿠폰 상세 캐시(JSON) 조회.
     *
     * @param couponId 쿠폰 ID
     * @return 캐시된 JSON, 없으면 empty
     */
    public Optional<String> getCouponDetail(final Long couponId) {
        String key = COUPON_DETAIL_KEY_PREFIX + couponId;
        try {
            String value = stringRedisTemplate.opsForValue().get(key);
            return Optional.ofNullable(value);
        } catch (Exception e) {
            throw new RedisConnectionException("쿠폰 상세 캐시 조회 실패: " + key, e);
        }
    }

    /**
     * 쿠폰 상세 캐시 저장. TTL 갱신.
     *
     * @param couponId   쿠폰 ID
     * @param jsonValue  JSON 문자열
     * @param ttlSeconds TTL(초)
     */
    public void setCouponDetail(final Long couponId, final String jsonValue, final long ttlSeconds) {
        String key = COUPON_DETAIL_KEY_PREFIX + couponId;
        try {
            stringRedisTemplate.opsForValue().set(key, jsonValue, java.time.Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            throw new RedisConnectionException("쿠폰 상세 캐시 저장 실패: " + key, e);
        }
    }

    /** 쿠폰 상세 캐시 기본 TTL(초) */
    public static final long COUPON_DETAIL_TTL_SECONDS = 3600L;

    // ----- coupon:active:{couponId} (project.mdc 3.4) -----

    /**
     * 쿠폰 활성(발급 가능) 상태를 TTL과 함께 설정한다.
     *
     * @param couponId   쿠폰 ID
     * @param ttlSeconds 유효 시간(초)
     */
    public void setCouponActive(final Long couponId, final long ttlSeconds) {
        String key = COUPON_ACTIVE_KEY_PREFIX + couponId;
        try {
            stringRedisTemplate.opsForValue().set(key, "true", java.time.Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            throw new RedisConnectionException("쿠폰 활성 상태 저장 실패: " + key, e);
        }
    }

    /**
     * Redis에 쿠폰이 활성인지 조회한다.
     *
     * @param couponId 쿠폰 ID
     * @return 활성이면 true
     */
    public boolean isCouponActive(final Long couponId) {
        String key = COUPON_ACTIVE_KEY_PREFIX + couponId;
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
        } catch (Exception e) {
            throw new RedisConnectionException("쿠폰 활성 조회 실패: " + key, e);
        }
    }

    private static String stockKey(final Long couponId) {
        return STOCK_KEY_PREFIX + couponId + STOCK_KEY_SUFFIX;
    }

    private static String issuedUsersKey(final Long couponId) {
        return STOCK_KEY_PREFIX + couponId + ISSUED_USERS_KEY_SUFFIX;
    }

    private static String eventActiveKey(final Long eventId) {
        return EVENT_ACTIVE_KEY_PREFIX + eventId + EVENT_ACTIVE_SUFFIX;
    }
}
