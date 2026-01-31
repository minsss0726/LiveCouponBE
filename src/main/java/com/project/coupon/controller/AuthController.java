package com.project.coupon.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.coupon.dto.LoginRequest;
import com.project.coupon.dto.LoginResponse;
import com.project.coupon.exception.ErrorResponse;
import com.project.coupon.security.CustomUserDetails;

/**
 * 인증 API (JSON 로그인).
 * SPA에서 JSON으로 로그인 요청 시 리다이렉트 없이 JSON 응답을 반환합니다.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final org.springframework.security.authentication.AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(AuthenticationConfiguration authenticationConfiguration,
                          SecurityContextRepository securityContextRepository) throws Exception {
        this.authenticationManager = authenticationConfiguration.getAuthenticationManager();
        this.securityContextRepository = securityContextRepository;
    }

    /**
     * JSON 로그인. 성공 시 세션 생성 및 LoginResponse 반환, 실패 시 401 + ErrorResponse.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getLoginId(), request.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(auth);
            httpRequest.getSession(true);
            securityContextRepository.saveContext(SecurityContextHolder.getContext(), httpRequest, httpResponse);

            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            LoginResponse body = new LoginResponse(
                    userDetails.getUserId(),
                    userDetails.getUsername());
            return ResponseEntity.ok(body);
        } catch (BadCredentialsException e) {
            ErrorResponse error = ErrorResponse.of(
                    "UNAUTHORIZED",
                    "로그인 ID 또는 비밀번호가 올바르지 않습니다.",
                    httpRequest.getRequestURI());
            return ResponseEntity.status(401).body(error);
        }
    }
}
