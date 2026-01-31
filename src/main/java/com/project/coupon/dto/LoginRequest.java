package com.project.coupon.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인 API 요청 DTO.
 * JSON body (loginId, password) for POST /auth/login.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class LoginRequest {

    @NotBlank(message = "로그인 ID를 입력하세요.")
    private String loginId;

    @NotBlank(message = "비밀번호를 입력하세요.")
    private String password;
}
