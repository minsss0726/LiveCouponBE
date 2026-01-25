package com.project.coupon.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 이벤트 엔티티
 * 실제 쿠폰을 발급하기 위한 이벤트 정보를 저장
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "events")
public class Events extends BaseTime {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;
    
    @Column(name = "event_name", nullable = false, length = 255)
    private String eventName;
    
    @Column(name = "event_detail", columnDefinition = "TEXT")
    private String eventDetail;
    
    @Column(name = "event_start_datetime", nullable = false)
    private LocalDateTime eventStartDatetime;
    
    @Column(name = "event_end_datetime", nullable = false)
    private LocalDateTime eventEndDatetime;
}
