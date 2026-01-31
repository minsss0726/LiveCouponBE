package com.project.coupon.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.coupon.security.CustomUserDetails;
import com.project.coupon.service.CouponService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {
    
    private final CouponService couponService;

    /**
     * 쿠폰 발급
     * @param couponId 쿠폰 ID
     * @return 쿠폰 발급 결과
     */
    @PostMapping("/{couponId}/issue")
    public ResponseEntity<Void> issueCoupon(@PathVariable final Long couponId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUserId();

        couponService.issueCoupon(userId, couponId);
        
        return ResponseEntity.ok().build();
    }
}
