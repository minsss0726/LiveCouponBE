package com.project.coupon.exception;

/**
 * 잘못된 요청이 들어왔을 때 발생하는 예외
 * 
 * <p>요청 파라미터가 유효하지 않거나, 비즈니스 규칙에 위배되는 요청일 때 발생합니다.
 * 예: null 값, 음수 값, 범위를 벗어난 값 등
 */
public final class InvalidRequestException extends BaseException {
    
    private static final String ERROR_CODE = "INVALID_REQUEST";
    
    /**
     * 잘못된 요청 예외를 생성합니다.
     * 
     * @param message 오류 메시지
     */
    public InvalidRequestException(final String message) {
        super(ERROR_CODE, message);
    }
    
    /**
     * 잘못된 요청 예외를 생성합니다.
     * 
     * @param fieldName 필드명
     * @param value 필드 값
     */
    public InvalidRequestException(final String fieldName, final Object value) {
        super(ERROR_CODE, String.format("잘못된 요청입니다. 필드: %s, 값: %s", fieldName, value));
    }
    
    /**
     * 잘못된 요청 예외를 생성합니다.
     * 
     * @param message 오류 메시지
     * @param cause 원인 예외
     */
    public InvalidRequestException(final String message, final Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}
