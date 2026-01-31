package com.project.coupon.dto;

import com.project.coupon.entity.UserCoupon;
import com.project.coupon.entity.enums.CouponStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 보유 쿠폰 한 건에 대한 응답 DTO.
 * 쿠폰 정보와 사용 상태를 담는다.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserCouponItemResponse {

    private Long userCouponId;
    private Long couponId;
    private String couponName;
    private CouponStatus couponStatus;

    /**
     * UserCoupon entity를 응답 DTO로 변환합니다.
     *
     * @param userCoupon UserCoupon entity
     * @return UserCouponItemResponse
     */
    public static UserCouponItemResponse from(final UserCoupon userCoupon) {
        return UserCouponItemResponse.builder()
            .userCouponId(userCoupon.getUserCouponId())
            .couponId(userCoupon.getCoupon().getCouponId())
            .couponName(userCoupon.getCoupon().getCouponName())
            .couponStatus(userCoupon.getCouponStatus())
            .build();
    }
}
