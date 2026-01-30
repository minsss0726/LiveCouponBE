package com.project.coupon.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.project.coupon.dto.EventResponse;
import com.project.coupon.entity.Events;
import com.project.coupon.exception.EventNotFoundException;
import com.project.coupon.repository.EventsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventsRepository eventsRepository;

    /**
     * 전체 이벤트 조회
     *
     * @return 전체 이벤트 목록 (없으면 빈 리스트)
     */
    public List<EventResponse> getEvents() {
        return eventsRepository.findAll().stream()
            .map(EventResponse::from)
            .toList();
    }

    /**
     * 이벤트 ID 로 이벤트 조회
     *
     * @param eventId 이벤트 ID
     * @return 이벤트 응답 DTO
     * @throws EventNotFoundException 이벤트가 없을 때
     */
    public EventResponse getEventById(final Long eventId) {
        Events event = eventsRepository.findById(eventId)
            .orElseThrow(() -> new EventNotFoundException(eventId));
        return EventResponse.from(event);
    }
}
