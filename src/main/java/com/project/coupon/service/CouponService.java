package com.project.coupon.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.coupon.dto.CouponCacheDto;
import com.project.coupon.entity.Coupons;
import com.project.coupon.entity.Events;
import com.project.coupon.entity.UserCoupon;
import com.project.coupon.entity.Users;
import com.project.coupon.entity.enums.CouponStatus;
import com.project.coupon.exception.CouponExhaustedException;
import com.project.coupon.exception.CouponNotFoundException;
import com.project.coupon.exception.DuplicateCouponException;
import com.project.coupon.exception.EventExpiredException;
import com.project.coupon.exception.TooManyRequestsException;
import com.project.coupon.exception.UserNotFoundException;
import com.project.coupon.repository.CouponsRepository;
import com.project.coupon.repository.UserCouponRepository;
import com.project.coupon.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponService {

    private static final Logger log = LoggerFactory.getLogger(CouponService.class);

    private final CouponsRepository couponsRepository;
    private final UsersRepository usersRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponRedisService couponRedisService;
    private final ObjectMapper objectMapper;

    /**
     * 캐시 또는 DB에서 쿠폰을 조회한다. project.mdc 3.3: 캐시 히트 시 DB 미조회.
     */
    public Coupons getCouponFromCacheOrDb(final Long couponId) {
        var cached = couponRedisService.getCouponDetail(couponId);
        if (cached.isPresent()) {
            try {
                CouponCacheDto dto = objectMapper.readValue(cached.get(), CouponCacheDto.class);
                return toCouponFromCache(dto);
            } catch (JsonProcessingException e) {
                log.warn("Coupon cache deserialize failed, couponId: {}", couponId, e);
            }
        }
        Coupons coupon = couponsRepository.findById(couponId)
            .orElseThrow(() -> new CouponNotFoundException(couponId));
        putCouponDetailCache(coupon);
        return coupon;
    }

    private void putCouponDetailCache(final Coupons coupon) {
        CouponCacheDto dto = CouponCacheDto.builder()
            .couponId(coupon.getCouponId())
            .eventId(coupon.getEvent().getEventId())
            .couponName(coupon.getCouponName())
            .couponDetail(coupon.getCouponDetail())
            .couponApplyStartDatetime(coupon.getCouponApplyStartDatetime())
            .couponApplyEndDatetime(coupon.getCouponApplyEndDatetime())
            .couponTotalCount(coupon.getCouponTotalCount())
            .eventStartDatetime(coupon.getEvent().getEventStartDatetime())
            .eventEndDatetime(coupon.getEvent().getEventEndDatetime())
            .eventName(coupon.getEvent().getEventName())
            .build();
        try {
            String json = objectMapper.writeValueAsString(dto);
            couponRedisService.setCouponDetail(coupon.getCouponId(), json, CouponRedisService.COUPON_DETAIL_TTL_SECONDS);
        } catch (JsonProcessingException e) {
            log.warn("Coupon cache serialize failed, couponId: {}", coupon.getCouponId(), e);
        }
    }

    private static Coupons toCouponFromCache(final CouponCacheDto dto) {
        Events event = Events.builder()
            .eventId(dto.getEventId())
            .eventName(dto.getEventName() != null ? dto.getEventName() : "")
            .eventStartDatetime(dto.getEventStartDatetime())
            .eventEndDatetime(dto.getEventEndDatetime())
            .build();
        return Coupons.builder()
            .couponId(dto.getCouponId())
            .event(event)
            .couponName(dto.getCouponName())
            .couponDetail(dto.getCouponDetail())
            .couponApplyStartDatetime(dto.getCouponApplyStartDatetime())
            .couponApplyEndDatetime(dto.getCouponApplyEndDatetime())
            .couponTotalCount(dto.getCouponTotalCount())
            .build();
    }

    /**
     * 유저에게 쿠폰 발급.
     * redis.mdc 플로우: 인증 → Rate limit → 이벤트 활성 → Lua → DB 저장.
     *
     * @param userId   유저 ID
     * @param couponId 쿠폰 ID
     * @param clientIp 클라이언트 IP (rate limit용, null 가능)
     */
    public void issueCoupon(final Long userId, final Long couponId, final String clientIp) {
        Coupons coupon = getCouponFromCacheOrDb(couponId);
        Users user = usersRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        LocalDateTime now = LocalDateTime.now();
        Long eventId = coupon.getEvent().getEventId();

        if (!couponRedisService.isEventActive(eventId)) {
            throw new EventExpiredException(eventId);
        }
        if (now.isBefore(coupon.getEvent().getEventStartDatetime())) {
            throw new EventExpiredException(eventId, coupon.getEvent().getEventStartDatetime());
        }
        if (now.isAfter(coupon.getEvent().getEventEndDatetime())) {
            throw new EventExpiredException(eventId,
                coupon.getEvent().getEventStartDatetime(),
                coupon.getEvent().getEventEndDatetime());
        }
        // 쿠폰 발급 가능 여부는 이벤트 기간만 검사. couponApplyStart/End는 발급과 무관(다른 코드에서 사용).

        if (!couponRedisService.checkAndIncrementRate(userId, clientIp)) {
            throw new TooManyRequestsException();
        }

        couponRedisService.ensureStockIfAbsent(couponId, coupon.getCouponTotalCount());
        int result = couponRedisService.tryIssue(couponId, userId);

        if (result == -1) {
            throw new DuplicateCouponException(userId, couponId);
        }
        if (result == 0) {
            throw new CouponExhaustedException(couponId);
        }

        Coupons couponRef = couponsRepository.getReferenceById(couponId);
        UserCoupon userCoupon = UserCoupon.builder()
            .user(user)
            .coupon(couponRef)
            .couponStatus(CouponStatus.NOT_USE)
            .build();
        try {
            userCouponRepository.save(userCoupon);
        } catch (Exception e) {
            couponRedisService.rollbackIssue(couponId, userId);
            log.error("Redis 발급 성공 후 DB 저장 실패. couponId: {}, userId: {}", couponId, userId, e);
            throw e;
        }
    }
}
