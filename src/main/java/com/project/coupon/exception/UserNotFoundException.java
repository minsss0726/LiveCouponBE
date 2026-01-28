package com.project.coupon.exception;

/**
 * 사용자를 찾을 수 없을 때 발생하는 예외
 * 
 * <p>요청한 사용자 ID에 해당하는 사용자가 데이터베이스에 존재하지 않을 때 발생합니다.
 */
public final class UserNotFoundException extends BaseException {
    
    private static final String ERROR_CODE = "USER_NOT_FOUND";
    
    /**
     * 사용자 없음 예외를 생성합니다.
     * 
     * @param userId 사용자 ID
     */
    public UserNotFoundException(final Long userId) {
        super(ERROR_CODE, String.format("사용자를 찾을 수 없습니다. userId: %d", userId));
    }
    
    /**
     * 사용자 없음 예외를 생성합니다.
     * 
     * @param userLoginId 사용자 로그인 ID
     */
    public UserNotFoundException(final String userLoginId) {
        super(ERROR_CODE, String.format("사용자를 찾을 수 없습니다. userLoginId: %s", userLoginId));
    }
    
    /**
     * 사용자 없음 예외를 생성합니다.
     * 
     * @param userId 사용자 ID
     * @param message 추가 메시지
     */
    public UserNotFoundException(final Long userId, final String message) {
        super(ERROR_CODE, String.format("사용자를 찾을 수 없습니다. userId: %d, %s", userId, message));
    }
}
