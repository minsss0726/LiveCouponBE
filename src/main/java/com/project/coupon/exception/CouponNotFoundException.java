package com.project.coupon.exception;

/**
 * 쿠폰을 찾을 수 없을 때 발생하는 예외
 * 
 * <p>요청한 쿠폰 ID에 해당하는 쿠폰이 데이터베이스나 Redis에 존재하지 않을 때 발생합니다.
 */
public final class CouponNotFoundException extends BaseException {
    
    private static final String ERROR_CODE = "COUPON_NOT_FOUND";
    
    /**
     * 쿠폰 없음 예외를 생성합니다.
     * 
     * @param couponId 쿠폰 ID
     */
    public CouponNotFoundException(final Long couponId) {
        super(ERROR_CODE, String.format("쿠폰을 찾을 수 없습니다. couponId: %d", couponId));
    }
    
    /**
     * 쿠폰 없음 예외를 생성합니다.
     * 
     * @param couponId 쿠폰 ID
     * @param message 추가 메시지
     */
    public CouponNotFoundException(final Long couponId, final String message) {
        super(ERROR_CODE, String.format("쿠폰을 찾을 수 없습니다. couponId: %d, %s", couponId, message));
    }
}
