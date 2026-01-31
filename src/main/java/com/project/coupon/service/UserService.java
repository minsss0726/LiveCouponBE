package com.project.coupon.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.coupon.dto.UserCouponItemResponse;
import com.project.coupon.dto.UserResponse;
import com.project.coupon.exception.UserNotFoundException;
import com.project.coupon.repository.UserCouponRepository;
import com.project.coupon.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UsersRepository usersRepository;
    private final UserCouponRepository userCouponRepository;

    /**
     * 페이지 진입 시 유저 정보 가져오기
     * 유저 이름 + 보유 쿠폰 목록, 상태
     *
     * @param userId 유저 ID
     * @return 유저 정보 (이름, 보유 쿠폰 목록 및 상태)
     */
    @Transactional(readOnly = true)
    public UserResponse getUserInfo(final Long userId) {
        final var user = usersRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        final List<UserCouponItemResponse> ownedCoupons = userCouponRepository.findByUser_UserIdWithCoupon(userId)
            .stream()
            .map(UserCouponItemResponse::from)
            .toList();

        return UserResponse.of(user, ownedCoupons);
    }
}
