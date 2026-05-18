package com.liveclass.dataengineering.domain.mysql;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "aggregation_result",
        uniqueConstraints = @UniqueConstraint(columnNames = {"agg_date", "event_type", "utm_medium"})
)
public class AggregationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 집계 날짜 */
    @Column(name = "agg_date", nullable = false)
    private LocalDate aggDate;

    /** 행동 타입 (PAGE_VIEW / CLICK / PURCHASE) */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /** 유입 채널 (광고 / 소셜미디어 / 웹검색 / 이메일 / 배너광고 / null=직접유입) */
    @Column(name = "utm_medium", length = 100)
    private String utmMedium;

    // ── PAGE_VIEW 유입 분석 ──────────────────────────
    /** 전체 이벤트 발생 건수 */
    @Column(nullable = false)
    private Long totalCount;

    /** 유니크 사용자 수 (비회원 포함) */
    @Column(nullable = false)
    private Long uniqueUsers;

    /** 비회원 유입 수 (userId = null) */
    @Column(nullable = false)
    private Long anonymousCount;

    // ── CLICK 행동 분석 ──────────────────────────────
    /** 가장 많이 클릭된 버튼/요소 */
    @Column(length = 200)
    private String topTargetId;

    // ── PURCHASE 구매 분석 ───────────────────────────
    /** 구매 건수 */
    @Column(nullable = false)
    private Long purchaseCount;

    /** 구매 전환율 (%) = purchaseCount / totalCount * 100 */
    @Column(precision = 5, scale = 2)
    private BigDecimal conversionRate;

    /** 총 구매 금액 */
    @Column(nullable = false)
    private Long totalAmount;

    /** 평균 구매 금액 */
    @Column(precision = 10, scale = 2)
    private BigDecimal avgAmount;

    // ── 체류시간 분석 ────────────────────────────────
    /** 총 체류시간 합계 (초) */
    @Column(nullable = false)
    private Long totalDurationSeconds;

    /** 평균 체류시간 (초) */
    @Column(nullable = false)
    private Long avgDurationSeconds;

    /** 이탈 건수 (durationSeconds < 10초) */
    @Column(nullable = false)
    private Long bounceCount;

    // ── 이상 IP 탐지 ─────────────────────────────────
    /** 당일 가장 많이 접속한 IP */
    @Column(length = 50)
    private String topIp;

    /** 상위 IP 접속 횟수 */
    @Column(nullable = false)
    private Long topIpCount;

    // ── 메타 ─────────────────────────────────────────
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder
    public AggregationResult(LocalDate aggDate, String eventType, String utmMedium,
                             Long totalCount, Long uniqueUsers, Long anonymousCount,
                             String topTargetId,
                             Long purchaseCount, BigDecimal conversionRate,
                             Long totalAmount, BigDecimal avgAmount,
                             Long totalDurationSeconds, Long avgDurationSeconds, Long bounceCount,
                             String topIp, Long topIpCount) {
        this.aggDate = aggDate;
        this.eventType = eventType;
        this.utmMedium = utmMedium;
        this.totalCount = totalCount;
        this.uniqueUsers = uniqueUsers;
        this.anonymousCount = anonymousCount;
        this.topTargetId = topTargetId;
        this.purchaseCount = purchaseCount;
        this.conversionRate = conversionRate;
        this.totalAmount = totalAmount;
        this.avgAmount = avgAmount;
        this.totalDurationSeconds = totalDurationSeconds;
        this.avgDurationSeconds = avgDurationSeconds;
        this.bounceCount = bounceCount;
        this.topIp = topIp;
        this.topIpCount = topIpCount;
    }
}