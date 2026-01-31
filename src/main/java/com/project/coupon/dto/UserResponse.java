package com.project.coupon.dto;

import java.util.List;

import com.project.coupon.entity.Users;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 페이지 진입 시 조회하는 유저 정보 응답 DTO.
 * 유저 이름(로그인 ID)과 보유 쿠폰 목록·상태를 담는다.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserResponse {

    private Long userId;
    private String userName;
    private List<UserCouponItemResponse> ownedCoupons;

    /**
     * Users entity와 보유 쿠폰 목록으로 응답 DTO를 생성합니다.
     *
     * @param user Users entity
     * @param ownedCoupons 보유 쿠폰 목록 (각 항목에 상태 포함)
     * @return UserResponse
     */
    public static UserResponse of(final Users user, final List<UserCouponItemResponse> ownedCoupons) {
        return UserResponse.builder()
            .userId(user.getUserId())
            .userName(user.getUserLoginId())
            .ownedCoupons(ownedCoupons != null ? ownedCoupons : List.of())
            .build();
    }
}
