package com.project.coupon.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 쿠폰 엔티티
 * 이벤트에서 발급할 쿠폰 정보를 저장
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "coupons")
public class Coupons extends BaseTime {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long couponId;
    
    @Column(name = "coupon_name", nullable = false, length = 255)
    private String couponName;
    
    @Column(name = "coupon_detail", columnDefinition = "TEXT")
    private String couponDetail;
    
    @Column(name = "coupon_apply_start_datetime", nullable = false)
    private LocalDateTime couponApplyStartDatetime;
    
    @Column(name = "coupon_apply_end_datetime", nullable = false)
    private LocalDateTime couponApplyEndDatetime;
    
    @Column(name = "coupon_total_count", nullable = false)
    private Integer couponTotalCount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Events event;
}
