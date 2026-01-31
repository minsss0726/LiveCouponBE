package com.project.coupon.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.coupon.dto.EventResponse;
import com.project.coupon.service.EventService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final EventService eventService;
    
    /**
     * 관리자 페이지 진입 시 현재 등록된 이벤트 목록 가져오기
     * @return 이벤트 목록
     */
    @GetMapping
    public ResponseEntity<List<EventResponse>> getEvents() {
        return ResponseEntity.ok(eventService.getEvents());
    }

    /**
     * 이벤트 오픈 시 해당 이벤트의 모든 쿠폰 초기 재고를 Redis에 저장한다.
     * DB의 coupon_total_count(초기 개수)를 Redis에 세팅할 때 호출한다.
     *
     * @param eventId 이벤트 ID
     * @return 200 OK
     */
    @PostMapping("/{eventId}/initialize-coupons")
    public ResponseEntity<Void> initializeCouponStocks(@PathVariable final Long eventId) {
        eventService.initializeCouponStocksForEvent(eventId);
        return ResponseEntity.ok().build();
    }
}
