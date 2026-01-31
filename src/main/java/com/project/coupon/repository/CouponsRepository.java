package com.project.coupon.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.coupon.entity.Coupons;

@Repository
public interface CouponsRepository extends JpaRepository<Coupons, Long> {

    /**
     * 이벤트 ID 로 쿠폰 조회
     * @param eventId 이벤트 ID
     * @return 쿠폰 목록
     */
    List<Coupons> findAllByEvent_EventId(Long eventId);
    
}
