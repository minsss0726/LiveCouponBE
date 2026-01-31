package com.project.coupon.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.project.coupon.exception.RedisConnectionException;

import lombok.RequiredArgsConstructor;

/**
 * 쿠폰 발급의 선착순·중복 방지를 위한 Redis 연산 서비스.
 * Lua 스크립트로 중복 체크·재고 차감·발급 등록을 원자적으로 수행한다.
 */
@Service
@RequiredArgsConstructor
public class CouponRedisService {

    private static final String STOCK_KEY_PREFIX = "coupon:";
    private static final String STOCK_KEY_SUFFIX = ":stock";
    private static final String ISSUED_USERS_KEY_SUFFIX = ":issued_users";

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

    private static String stockKey(final Long couponId) {
        return STOCK_KEY_PREFIX + couponId + STOCK_KEY_SUFFIX;
    }

    private static String issuedUsersKey(final Long couponId) {
        return STOCK_KEY_PREFIX + couponId + ISSUED_USERS_KEY_SUFFIX;
    }
}
