package com.project.coupon.dto;

import java.time.LocalDateTime;

import com.project.coupon.entity.Events;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이벤트 API 응답 DTO.
 * Front 단에 노출하는 이벤트 정보.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EventResponse {

    private Long eventId;
    private String eventName;
    private String eventDetail;
    private LocalDateTime eventStartDatetime;
    private LocalDateTime eventEndDatetime;

    /**
     * Entity를 Response DTO로 변환합니다.
     *
     * @param event Events entity
     * @return EventResponse
     */
    public static EventResponse from(final Events event) {
        return EventResponse.builder()
            .eventId(event.getEventId())
            .eventName(event.getEventName())
            .eventDetail(event.getEventDetail())
            .eventStartDatetime(event.getEventStartDatetime())
            .eventEndDatetime(event.getEventEndDatetime())
            .build();
    }
}
