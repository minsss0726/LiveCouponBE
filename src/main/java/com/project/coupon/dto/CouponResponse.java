package com.project.coupon.dto;

import com.project.coupon.entity.Coupons;
import com.project.coupon.util.DateTimeFormatUtil;

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
    private String couponApplyStartDatetime;
    private String couponApplyEndDatetime;
    private Integer couponTotalCount;

    /**
     * Entity를 Response DTO로 변환합니다.
     * 날짜/시간은 "yyyy년 MM월 dd일 HH:mm:ss" 형식 문자열로 변환됩니다.
     *
     * @param coupon Coupons entity
     * @return CouponResponse
     */
    public static CouponResponse from(final Coupons coupon) {
        return CouponResponse.builder()
            .couponId(coupon.getCouponId())
            .couponName(coupon.getCouponName())
            .couponDetail(coupon.getCouponDetail())
            .couponApplyStartDatetime(DateTimeFormatUtil.format(coupon.getCouponApplyStartDatetime()))
            .couponApplyEndDatetime(DateTimeFormatUtil.format(coupon.getCouponApplyEndDatetime()))
            .couponTotalCount(coupon.getCouponTotalCount())
            .build();
    }
}
