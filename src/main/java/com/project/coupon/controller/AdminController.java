package com.project.coupon.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.coupon.dto.EventResponse;
import com.project.coupon.exception.ErrorResponse;
import com.project.coupon.service.EventService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Admin", description = "관리자용 이벤트·쿠폰 초기화 API")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final EventService eventService;
    
    @Operation(summary = "관리자 이벤트 목록", description = "관리자 페이지 진입 시 등록된 이벤트 목록을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공")
    })
    @GetMapping
    public ResponseEntity<List<EventResponse>> getEvents() {
        return ResponseEntity.ok(eventService.getEvents());
    }

    @Operation(summary = "쿠폰 재고 Redis 초기화", description = "이벤트 오픈 시 해당 이벤트의 모든 쿠폰 초기 재고를 Redis에 저장합니다. DB의 coupon_total_count를 Redis에 세팅할 때 호출합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "초기화 완료"),
            @ApiResponse(responseCode = "404", description = "이벤트 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{eventId}/initialize-coupons")
    public ResponseEntity<Void> initializeCouponStocks(
            @Parameter(description = "이벤트 ID") @PathVariable final Long eventId) {
        eventService.initializeCouponStocksForEvent(eventId);
        return ResponseEntity.ok().build();
    }
}
