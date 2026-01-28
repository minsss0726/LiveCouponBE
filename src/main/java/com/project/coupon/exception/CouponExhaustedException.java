package com.project.coupon.exception;

/**
 * 쿠폰 재고가 부족할 때 발생하는 예외
 * 
 * <p>Redis에서 쿠폰 수량을 차감한 결과가 음수일 때 발생합니다.
 * 이는 모든 쿠폰이 이미 발급되었음을 의미합니다.
 */
public final class CouponExhaustedException extends BaseException {
    
    private static final String ERROR_CODE = "COUPON_EXHAUSTED";
    
    /**
     * 쿠폰 재고 부족 예외를 생성합니다.
     * 
     * @param couponId 쿠폰 ID
     */
    public CouponExhaustedException(final Long couponId) {
        super(ERROR_CODE, String.format("쿠폰 재고가 부족합니다. couponId: %d", couponId));
    }
    
    /**
     * 쿠폰 재고 부족 예외를 생성합니다.
     * 
     * @param couponId 쿠폰 ID
     * @param message 추가 메시지
     */
    public CouponExhaustedException(final Long couponId, final String message) {
        super(ERROR_CODE, String.format("쿠폰 재고가 부족합니다. couponId: %d, %s", couponId, message));
    }
}
