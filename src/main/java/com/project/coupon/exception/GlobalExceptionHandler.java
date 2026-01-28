package com.project.coupon.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * 전역 예외 핸들러
 * 
 * <p>애플리케이션 전역에서 발생하는 예외를 일관된 형식으로 처리합니다.
 * 모든 예외를 ErrorResponse 형식으로 변환하여 클라이언트에게 반환합니다.
 * 
 * <p>Effective Java 가이드라인에 따라:
 * <ul>
 *   <li>예외를 무시하지 않고 적절히 처리</li>
 *   <li>실패 정보를 포함한 상세 메시지 제공</li>
 * </ul>
 */
@RestControllerAdvice
public final class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * BaseException 및 그 하위 예외를 처리합니다.
     * 
     * @param exception 예외 객체
     * @param request 웹 요청
     * @return 오류 응답
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(
            final BaseException exception, 
            final WebRequest request) {
        
        log.warn("Business exception occurred: {} - {}", 
            exception.getErrorCode(), exception.getMessage());
        
        final ErrorResponse errorResponse = ErrorResponse.from(exception, 
            request.getDescription(false).replace("uri=", ""));
        
        final HttpStatus status = determineHttpStatus(exception);
        
        return ResponseEntity.status(status).body(errorResponse);
    }
    
    /**
     * 요청 파라미터 검증 실패 예외를 처리합니다.
     * 
     * @param exception 예외 객체
     * @param request 웹 요청
     * @return 오류 응답
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            final MethodArgumentNotValidException exception, 
            final WebRequest request) {
        
        final StringBuilder message = new StringBuilder("요청 파라미터가 유효하지 않습니다. ");
        
        exception.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError fieldError) {
                message.append(String.format("[%s: %s] ", 
                    fieldError.getField(), 
                    fieldError.getDefaultMessage()));
            } else {
                message.append(String.format("[%s] ", error.getDefaultMessage()));
            }
        });
        
        log.warn("Validation exception: {}", message);
        
        final InvalidRequestException invalidRequestException = 
            new InvalidRequestException(message.toString().trim());
        
        final ErrorResponse errorResponse = ErrorResponse.from(invalidRequestException, 
            request.getDescription(false).replace("uri=", ""));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * 바인딩 예외를 처리합니다.
     * 
     * @param exception 예외 객체
     * @param request 웹 요청
     * @return 오류 응답
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(
            final BindException exception, 
            final WebRequest request) {
        
        final StringBuilder message = new StringBuilder("요청 바인딩 오류가 발생했습니다. ");
        
        exception.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError fieldError) {
                message.append(String.format("[%s: %s] ", 
                    fieldError.getField(), 
                    fieldError.getDefaultMessage()));
            } else {
                message.append(String.format("[%s] ", error.getDefaultMessage()));
            }
        });
        
        log.warn("Binding exception: {}", message);
        
        final InvalidRequestException invalidRequestException = 
            new InvalidRequestException(message.toString().trim());
        
        final ErrorResponse errorResponse = ErrorResponse.from(invalidRequestException, 
            request.getDescription(false).replace("uri=", ""));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * IllegalArgumentException을 처리합니다.
     * 
     * @param exception 예외 객체
     * @param request 웹 요청
     * @return 오류 응답
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            final IllegalArgumentException exception, 
            final WebRequest request) {
        
        log.warn("Illegal argument: {}", exception.getMessage());
        
        final InvalidRequestException invalidRequestException = 
            new InvalidRequestException(exception.getMessage());
        
        final ErrorResponse errorResponse = ErrorResponse.from(invalidRequestException, 
            request.getDescription(false).replace("uri=", ""));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * 모든 미처리 예외를 처리합니다.
     * 
     * @param exception 예외 객체
     * @param request 웹 요청
     * @return 오류 응답
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            final Exception exception, 
            final WebRequest request) {
        
        log.error("Unexpected exception occurred", exception);
        
        final ErrorResponse errorResponse = ErrorResponse.from(exception, 
            request.getDescription(false).replace("uri=", ""));
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * 예외 타입에 따라 적절한 HTTP 상태 코드를 결정합니다.
     * 
     * @param exception 예외 객체
     * @return HTTP 상태 코드
     */
    private HttpStatus determineHttpStatus(final BaseException exception) {
        return switch (exception.getErrorCode()) {
            case "COUPON_EXHAUSTED", "DUPLICATE_COUPON", "COUPON_EXPIRED" -> HttpStatus.CONFLICT;
            case "COUPON_NOT_FOUND", "USER_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "INVALID_REQUEST" -> HttpStatus.BAD_REQUEST;
            case "REDIS_CONNECTION_ERROR" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
