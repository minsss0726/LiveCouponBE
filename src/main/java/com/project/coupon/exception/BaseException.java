package com.project.coupon.exception;

/**
 * 프로젝트의 모든 커스텀 예외의 기본 클래스
 * 
 * <p>이 클래스는 모든 비즈니스 예외의 공통 기능을 제공합니다.
 * 예외 코드와 메시지를 포함하여 클라이언트에게 명확한 오류 정보를 전달합니다.
 * 
 * <p>Effective Java 가이드라인에 따라:
 * <ul>
 *   <li>실패 정보를 포함한 상세 메시지 제공</li>
 *   <li>적절한 추상화 레벨에서 예외 발생</li>
 *   <li>RuntimeException을 상속하여 unchecked exception으로 처리</li>
 * </ul>
 */
public abstract class BaseException extends RuntimeException {
    
    private final String errorCode;
    
    /**
     * 예외를 생성합니다.
     * 
     * @param errorCode 오류 코드
     * @param message 오류 메시지
     */
    protected BaseException(final String errorCode, final String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * 예외를 생성합니다.
     * 
     * @param errorCode 오류 코드
     * @param message 오류 메시지
     * @param cause 원인 예외
     */
    protected BaseException(final String errorCode, final String message, final Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * 오류 코드를 반환합니다.
     * 
     * @return 오류 코드
     */
    public String getErrorCode() {
        return errorCode;
    }
}
