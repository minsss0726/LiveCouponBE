package com.project.coupon.exception;

/**
 * 사용자가 이미 동일한 쿠폰을 발급받았을 때 발생하는 예외
 * 
 * <p>Redis에서 중복 발급 체크 시 이미 발급된 쿠폰이 존재할 때 발생합니다.
 * 프로젝트 요구사항에 따라 각 사용자는 동일한 쿠폰을 한 번만 발급받을 수 있습니다.
 */
public final class DuplicateCouponException extends BaseException {
    
    private static final String ERROR_CODE = "DUPLICATE_COUPON";
    
    /**
     * 중복 쿠폰 발급 예외를 생성합니다.
     * 
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     */
    public DuplicateCouponException(final Long userId, final Long couponId) {
        super(ERROR_CODE, String.format("이미 발급받은 쿠폰입니다. userId: %d, couponId: %d", userId, couponId));
    }
    
    /**
     * 중복 쿠폰 발급 예외를 생성합니다.
     * 
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @param message 추가 메시지
     */
    public DuplicateCouponException(final Long userId, final Long couponId, final String message) {
        super(ERROR_CODE, String.format("이미 발급받은 쿠폰입니다. userId: %d, couponId: %d, %s", 
            userId, couponId, message));
    }
}
