package com.project.coupon.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.coupon.entity.UserCoupon;

@Repository
public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    /**
     * 유저 ID로 해당 유저의 보유 쿠폰 목록을 쿠폰 정보와 함께 한 번에 조회합니다.
     *
     * @param userId 유저 ID
     * @return 보유 쿠폰 목록 (쿠폰 정보 포함)
     */
    @Query("SELECT uc FROM UserCoupon uc JOIN FETCH uc.coupon WHERE uc.user.userId = :userId")
    List<UserCoupon> findByUser_UserIdWithCoupon(@Param("userId") Long userId);
}
