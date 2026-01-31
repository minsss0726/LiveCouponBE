package com.project.coupon.dto;


import java.time.LocalDateTime;

import com.project.coupon.entity.Coupons;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CouponResponse {
    
    private Long couponId;
    private String couponName;
    private String couponDetail;
    private LocalDateTime couponApplyStartDatetime;
    private LocalDateTime couponApplyEndDatetime;
    private Integer couponTotalCount;

    /**
     * Entity를 Response DTO로 변환합니다.
     *
     * @param coupon Coupons entity
     * @return CouponResponse
     */
    public static CouponResponse from(final Coupons coupon) {
        return CouponResponse.builder()
            .couponId(coupon.getCouponId())
            .couponName(coupon.getCouponName())
            .couponDetail(coupon.getCouponDetail())
            .couponApplyStartDatetime(coupon.getCouponApplyStartDatetime())
            .couponApplyEndDatetime(coupon.getCouponApplyEndDatetime())
            .couponTotalCount(coupon.getCouponTotalCount())
            .build();
    }
}
