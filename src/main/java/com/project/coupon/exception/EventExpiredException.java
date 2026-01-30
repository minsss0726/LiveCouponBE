package com.project.coupon.exception;

import java.time.LocalDateTime;

/**
 * 이벤트 기간이 만료되었을 때 발생하는 예외
 *
 * <p>이벤트의 진행 기간(eventStartDatetime ~ eventEndDatetime)이
 * 지났거나 아직 시작되지 않았을 때 발생합니다.
 */
public final class EventExpiredException extends BaseException {

    private static final String ERROR_CODE = "EVENT_EXPIRED";

    /**
     * 이벤트 만료 예외를 생성합니다.
     *
     * @param eventId 이벤트 ID
     */
    public EventExpiredException(final Long eventId) {
        super(ERROR_CODE, String.format("이벤트 기간이 만료되었습니다. eventId: %d", eventId));
    }

    /**
     * 이벤트 만료 예외를 생성합니다.
     *
     * @param eventId 이벤트 ID
     * @param startDateTime 이벤트 시작 일시
     * @param endDateTime 이벤트 종료 일시
     */
    public EventExpiredException(final Long eventId,
                                 final LocalDateTime startDateTime,
                                 final LocalDateTime endDateTime) {
        super(ERROR_CODE, String.format(
            "이벤트 기간이 만료되었습니다. eventId: %d, 이벤트 기간: %s ~ %s",
            eventId, startDateTime, endDateTime));
    }

    /**
     * 이벤트 시작 전 예외를 생성합니다.
     *
     * @param eventId 이벤트 ID
     * @param startDateTime 이벤트 시작 일시
     */
    public EventExpiredException(final Long eventId, final LocalDateTime startDateTime) {
        super(ERROR_CODE, String.format(
            "이벤트가 아직 시작되지 않았습니다. eventId: %d, 이벤트 시작 일시: %s",
            eventId, startDateTime));
    }
}
