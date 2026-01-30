package com.project.coupon.exception;

/**
 * 이벤트를 찾을 수 없을 때 발생하는 예외
 *
 * <p>요청한 이벤트 ID에 해당하는 이벤트가 데이터베이스에 존재하지 않을 때 발생합니다.
 */
public final class EventNotFoundException extends BaseException {

    private static final String ERROR_CODE = "EVENT_NOT_FOUND";

    /**
     * 이벤트 없음 예외를 생성합니다.
     *
     * @param eventId 이벤트 ID
     */
    public EventNotFoundException(final Long eventId) {
        super(ERROR_CODE, String.format("이벤트를 찾을 수 없습니다. eventId: %d", eventId));
    }

    /**
     * 이벤트 없음 예외를 생성합니다.
     *
     * @param eventId 이벤트 ID
     * @param message 추가 메시지
     */
    public EventNotFoundException(final Long eventId, final String message) {
        super(ERROR_CODE, String.format("이벤트를 찾을 수 없습니다. eventId: %d, %s", eventId, message));
    }
}
