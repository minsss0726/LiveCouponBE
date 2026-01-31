package com.project.coupon.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.coupon.dto.CouponResponse;
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

    /**
     * 이벤트 ID 로 쿠폰 목록 조회
     * @param eventId 이벤트 ID
     * @return 쿠폰 목록
     */
    @GetMapping("/{eventId}/coupons")
    public ResponseEntity<List<CouponResponse>> getCouponsByEventId(@PathVariable final Long eventId) {
        return ResponseEntity.ok(eventService.getCouponsByEventId(eventId));
    }
    
    /**
     * 이벤트 오픈 시 해당 이벤트의 모든 쿠폰 초기 재고를 Redis에 저장한다.
     * DB의 coupon_total_count(초기 개수)를 Redis에 세팅할 때 호출한다.
     *
     * @param eventId 이벤트 ID
     * @return 204 No Content
     */
    @PostMapping("/{eventId}/initialize-coupons")
    public ResponseEntity<Void> initializeCouponStocks(@PathVariable final Long eventId) {
        eventService.initializeCouponStocksForEvent(eventId);
        return ResponseEntity.noContent().build();
    }
}
