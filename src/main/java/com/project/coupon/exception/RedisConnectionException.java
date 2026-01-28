package com.project.coupon.exception;

/**
 * Redis 연결 오류가 발생했을 때 발생하는 예외
 * 
 * <p>Redis 서버와의 연결이 실패하거나, Redis 작업 중 오류가 발생했을 때 사용됩니다.
 * 이 예외는 시스템 레벨의 오류이므로 원인 예외를 포함하는 것이 좋습니다.
 */
public final class RedisConnectionException extends BaseException {
    
    private static final String ERROR_CODE = "REDIS_CONNECTION_ERROR";
    
    /**
     * Redis 연결 오류 예외를 생성합니다.
     * 
     * @param message 오류 메시지
     */
    public RedisConnectionException(final String message) {
        super(ERROR_CODE, String.format("Redis 연결 오류가 발생했습니다. %s", message));
    }
    
    /**
     * Redis 연결 오류 예외를 생성합니다.
     * 
     * @param message 오류 메시지
     * @param cause 원인 예외
     */
    public RedisConnectionException(final String message, final Throwable cause) {
        super(ERROR_CODE, String.format("Redis 연결 오류가 발생했습니다. %s", message), cause);
    }
}
