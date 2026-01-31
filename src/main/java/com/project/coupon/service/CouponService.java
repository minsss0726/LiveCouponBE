package com.project.coupon.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.project.coupon.entity.Coupons;
import com.project.coupon.entity.UserCoupon;
import com.project.coupon.entity.Users;
import com.project.coupon.entity.enums.CouponStatus;
import com.project.coupon.exception.CouponExhaustedException;
import com.project.coupon.exception.CouponExpiredException;
import com.project.coupon.exception.CouponNotFoundException;
import com.project.coupon.exception.DuplicateCouponException;
import com.project.coupon.exception.UserNotFoundException;
import com.project.coupon.repository.CouponsRepository;
import com.project.coupon.repository.UserCouponRepository;
import com.project.coupon.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponsRepository couponsRepository;

    private final UsersRepository usersRepository;

    private final UserCouponRepository userCouponRepository;

    private final CouponRedisService couponRedisService;

    /**
     * 유저에게 쿠폰 발급
     * @param userId 유저 ID
     * @param couponId 쿠폰 ID
     * @throws CouponNotFoundException 쿠폰이 없을 때
     * @throws UserNotFoundException 유저가 없을 때
     * @throws CouponExpiredException 쿠폰 발급 기간이 만료되었을 때
     * @throws CouponExhaustedException 쿠폰 재고가 부족할 때
     * @throws DuplicateCouponException 쿠폰이 이미 발급되었을 때
     */
    public void issueCoupon(final Long userId, final Long couponId) {
        Coupons coupon = couponsRepository.findById(couponId)
            .orElseThrow(() -> new CouponNotFoundException(couponId));
        Users user = usersRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        
        LocalDateTime now = LocalDateTime.now();
        
        if (now.isBefore(coupon.getCouponApplyStartDatetime()) ||
            now.isAfter(coupon.getCouponApplyEndDatetime())) {
            throw new CouponExpiredException(couponId);
        }

        couponRedisService.ensureStockIfAbsent(couponId, coupon.getCouponTotalCount());
        int result = couponRedisService.tryIssue(couponId, userId);

        if (result == -1) {
            throw new DuplicateCouponException(userId, couponId);
        }
        if (result == 0) {
            throw new CouponExhaustedException(couponId);
        }

        UserCoupon userCoupon = UserCoupon.builder()
            .user(user)
            .coupon(coupon)
            .couponStatus(CouponStatus.NOT_USE)
            .build();
        userCouponRepository.save(userCoupon);
    }
}
