package com.project.coupon.exception;

/**
 * Rate limit 초과 시 발생하는 예외.
 * redis.mdc: rate:user:{userId}, rate:ip:{ipAddress} 기반 요청 제한.
 */
public final class TooManyRequestsException extends BaseException {

    private static final String ERROR_CODE = "RATE_LIMIT_EXCEEDED";

    public TooManyRequestsException() {
        super(ERROR_CODE, "요청 한도를 초과했습니다. 잠시 후 다시 시도해 주세요.");
    }

    public TooManyRequestsException(final String message) {
        super(ERROR_CODE, message);
    }
}
