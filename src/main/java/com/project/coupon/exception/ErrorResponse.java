package com.project.coupon.exception;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * API 오류 응답을 나타내는 DTO
 * 
 * <p>클라이언트에게 일관된 형식의 오류 정보를 제공합니다.
 * 불변 객체로 설계하여 데이터 무결성을 보장합니다.
 * 
 * <p>Data-Oriented Programming 원칙에 따라:
 * <ul>
 *   <li>데이터는 불변 객체로 표현</li>
 *   <li>제네릭 데이터 구조 사용</li>
 * </ul>
 */
public final class ErrorResponse {
    
    private final String errorCode;
    private final String message;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime timestamp;
    
    private final String path;
    
    /**
     * 오류 응답을 생성합니다.
     * 
     * @param errorCode 오류 코드
     * @param message 오류 메시지
     * @param path 요청 경로
     */
    private ErrorResponse(final String errorCode, final String message, final String path) {
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.path = path;
    }
    
    /**
     * BaseException으로부터 ErrorResponse를 생성합니다.
     * 
     * @param exception 예외 객체
     * @param path 요청 경로
     * @return ErrorResponse 인스턴스
     */
    public static ErrorResponse from(final BaseException exception, final String path) {
        return new ErrorResponse(exception.getErrorCode(), exception.getMessage(), path);
    }
    
    /**
     * 일반 예외로부터 ErrorResponse를 생성합니다.
     * 
     * @param exception 예외 객체
     * @param path 요청 경로
     * @return ErrorResponse 인스턴스
     */
    public static ErrorResponse from(final Exception exception, final String path) {
        return new ErrorResponse("INTERNAL_SERVER_ERROR", 
            "서버 내부 오류가 발생했습니다.", path);
    }
    
    /**
     * 오류 코드를 반환합니다.
     * 
     * @return 오류 코드
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * 오류 메시지를 반환합니다.
     * 
     * @return 오류 메시지
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * 오류 발생 시각을 반환합니다.
     * 
     * @return 오류 발생 시각
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * 요청 경로를 반환합니다.
     * 
     * @return 요청 경로
     */
    public String getPath() {
        return path;
    }
}
