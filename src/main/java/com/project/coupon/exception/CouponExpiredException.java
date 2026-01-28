package com.project.coupon.exception;

import java.time.LocalDateTime;

/**
 * 쿠폰 발급 기간이 만료되었을 때 발생하는 예외
 * 
 * <p>쿠폰의 발급 가능 기간(couponApplyStartDatetime ~ couponApplyEndDatetime)이
 * 지났거나 아직 시작되지 않았을 때 발생합니다.
 */
public final class CouponExpiredException extends BaseException {
    
    private static final String ERROR_CODE = "COUPON_EXPIRED";
    
    /**
     * 쿠폰 만료 예외를 생성합니다.
     * 
     * @param couponId 쿠폰 ID
     */
    public CouponExpiredException(final Long couponId) {
        super(ERROR_CODE, String.format("쿠폰 발급 기간이 만료되었습니다. couponId: %d", couponId));
    }
    
    /**
     * 쿠폰 만료 예외를 생성합니다.
     * 
     * @param couponId 쿠폰 ID
     * @param startDateTime 발급 시작 일시
     * @param endDateTime 발급 종료 일시
     */
    public CouponExpiredException(final Long couponId, 
                                  final LocalDateTime startDateTime, 
                                  final LocalDateTime endDateTime) {
        super(ERROR_CODE, String.format(
            "쿠폰 발급 기간이 만료되었습니다. couponId: %d, 발급 기간: %s ~ %s", 
            couponId, startDateTime, endDateTime));
    }
    
    /**
     * 쿠폰 발급 시작 전 예외를 생성합니다.
     * 
     * @param couponId 쿠폰 ID
     * @param startDateTime 발급 시작 일시
     */
    public CouponExpiredException(final Long couponId, final LocalDateTime startDateTime) {
        super(ERROR_CODE, String.format(
            "쿠폰 발급이 아직 시작되지 않았습니다. couponId: %d, 발급 시작 일시: %s", 
            couponId, startDateTime));
    }
}
