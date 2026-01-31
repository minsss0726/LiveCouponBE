package com.project.coupon.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.project.coupon.dto.CouponResponse;
import com.project.coupon.dto.EventResponse;
import com.project.coupon.entity.Coupons;
import com.project.coupon.entity.Events;
import com.project.coupon.exception.EventNotFoundException;
import com.project.coupon.repository.CouponsRepository;
import com.project.coupon.repository.EventsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventsRepository eventsRepository;
    private final CouponsRepository couponsRepository;
    private final CouponRedisService couponRedisService;

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

    /**
     * 이벤트 ID로 쿠폰 목록 조회
     * @param eventId 이벤트 ID
     * @return 쿠폰 목록
     */
    public List<CouponResponse> getCouponsByEventId(final Long eventId) {
        return couponsRepository.findAllByEvent_EventId(eventId).stream()
            .map(CouponResponse::from)
            .toList();
    }

    /**
     * 이벤트가 열릴 때 해당 이벤트의 모든 쿠폰 초기 재고를 Redis에 저장한다.
     * DB의 coupon_total_count(초기 개수)를 Redis coupon:{couponId}:stock 에 세팅한다.
     *
     * @param eventId 이벤트 ID
     */
    public void initializeCouponStocksForEvent(final Long eventId) {
        List<Coupons> coupons = couponsRepository.findAllByEvent_EventId(eventId);
        for (Coupons coupon : coupons) {
            couponRedisService.initializeStock(coupon.getCouponId(), coupon.getCouponTotalCount());
        }
    }
}
