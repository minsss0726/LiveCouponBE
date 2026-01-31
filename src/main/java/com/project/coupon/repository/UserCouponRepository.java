package com.project.coupon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.coupon.entity.UserCoupon;

@Repository
public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {
    
}
