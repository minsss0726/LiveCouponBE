package com.project.coupon.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.coupon.dto.UserResponse;
import com.project.coupon.security.CustomUserDetails;
import com.project.coupon.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;

    /**
     * 페이지 진입 시 유저 정보 가져오기
     * 유저 이름 + 보유 쿠폰 목록, 상태
     * @return 유저 정보
     */
    @GetMapping("/mypage")
    public ResponseEntity<UserResponse> getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUserId();
        return ResponseEntity.ok(userService.getUserInfo(userId));
    }
}
