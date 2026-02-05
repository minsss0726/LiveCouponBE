package com.project.coupon.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.coupon.exception.ErrorResponse;
import com.project.coupon.security.CustomUserDetails;
import com.project.coupon.service.CouponService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Tag(name = "Coupon", description = "쿠폰 발급 API")
@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private final CouponService couponService;

    @Operation(summary = "쿠폰 발급 (테스트)", description = "로그인 없이 userId를 path로 넘겨 쿠폰 발급 테스트. 인증 불필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "발급 성공"),
            @ApiResponse(responseCode = "409", description = "재고 소진 / 중복 발급 / 이벤트 종료 등", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "쿠폰/이벤트 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "요청 제한 초과 (Rate limit)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{couponId}/issue/{userId}")
    public ResponseEntity<Void> issueCouponForTest(
            @Parameter(description = "쿠폰 ID") @PathVariable("couponId") final Long couponId,
            @Parameter(description = "사용자 ID (테스트용)") @PathVariable("userId") final Long userId,
            final HttpServletRequest request) {
        String clientIp = resolveClientIp(request);
        couponService.issueCoupon(userId, couponId, clientIp);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "쿠폰 발급", description = "선착순 쿠폰 발급. Rate limit은 유저 ID 및 클라이언트 IP 기준으로 적용됩니다. 로그인 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "발급 성공"),
            @ApiResponse(responseCode = "409", description = "재고 소진 / 중복 발급 / 이벤트 종료 등", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "쿠폰/이벤트 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "요청 제한 초과 (Rate limit)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{couponId}/issue")
    public ResponseEntity<Void> issueCoupon(
            @Parameter(description = "쿠폰 ID") @PathVariable("couponId") final Long couponId,
            final HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUserId();
        String clientIp = resolveClientIp(request);

        couponService.issueCoupon(userId, couponId, clientIp);

        return ResponseEntity.ok().build();
    }

    private static String resolveClientIp(final HttpServletRequest request) {
        String forwarded = request.getHeader(X_FORWARDED_FOR);
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
