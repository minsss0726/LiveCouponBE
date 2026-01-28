package com.project.coupon.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.coupon.entity.Users;

/**
 * Users 엔티티용 JPA 리포지토리.
 */
public interface UsersRepository extends JpaRepository<Users, Long> {

    /**
     * 로그인 ID 로 사용자 조회.
     *
     * @param userLoginId 로그인 ID
     * @return 일치하는 사용자가 있으면 Optional 로 래핑된 Users
     */
    Optional<Users> findByUserLoginId(String userLoginId);
}

