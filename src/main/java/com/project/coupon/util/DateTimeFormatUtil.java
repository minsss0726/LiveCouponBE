package com.project.coupon.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Date/Time 포맷 유틸리티.
 * 프론트엔드 노출용 "년/월/일 시:분:초" 형식 문자열 변환.
 */
public final class DateTimeFormatUtil {

    /** 프론트 노출용: yyyy년 MM월 dd일 HH:mm:ss */
    private static final DateTimeFormatter DISPLAY_FORMAT =
        DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm:ss");

    private DateTimeFormatUtil() {
    }

    /**
     * LocalDateTime을 "yyyy년 MM월 dd일 HH:mm:ss" 형식 문자열로 변환한다.
     *
     * @param dateTime 변환할 시각 (null 허용)
     * @return 포맷된 문자열, null이면 null 반환
     */
    public static String format(final LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DISPLAY_FORMAT);
    }
}
