package com.project.coupon.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.coupon.dto.EventResponse;
import com.project.coupon.service.EventService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    /**
     * 전체 이벤트 조회
     *
     * @return 전체 이벤트 목록
     */
    @GetMapping
    public ResponseEntity<List<EventResponse>> getEvents() {
        return ResponseEntity.ok(eventService.getEvents());
    }

    /**
     * 이벤트 ID 로 이벤트 조회
     *
     * @param eventId 이벤트 ID
     * @return 이벤트 응답
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable final Long eventId) {
        return ResponseEntity.ok(eventService.getEventById(eventId));
    }
}
