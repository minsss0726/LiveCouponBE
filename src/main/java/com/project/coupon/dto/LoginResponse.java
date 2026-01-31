package com.project.coupon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인 API 성공 응답 DTO.
 * Matches frontend LoginResponse (userId, userName).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class LoginResponse {

    private Long userId;
    private String userName;
}
