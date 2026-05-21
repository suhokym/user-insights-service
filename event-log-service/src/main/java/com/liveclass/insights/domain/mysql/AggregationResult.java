package com.liveclass.insights.domain.mysql;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "aggregation_result",
        uniqueConstraints = @UniqueConstraint(columnNames = {"agg_date", "utm_medium"})
)
public class AggregationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 집계 날짜 */
    @Column(name = "agg_date", nullable = false)
    private LocalDate aggDate;

    /** 유입 채널 (광고 / 소셜미디어 / 웹검색 / 이메일 / 배너광고 / null=직접유입) */
    @Column(name = "utm_medium", length = 100)
    private String utmMedium;

    // ── PAGE_VIEW 유입 분석 ──────────────────────
    /** 전체 이벤트 발생 건수 */
    @Column(nullable = false)
    private Long totalCount;


    // ── PURCHASE 구매 분석 ───────────────────────
    /** 구매 건수 */
    @Column(nullable = false)
    private Long purchaseCount;

    /** 구매 전환율 (%) */
    @Column(precision = 5, scale = 2)
    private BigDecimal conversionRate;

    /** 총 구매 금액 */
    @Column(nullable = false)
    private Long totalAmount;

    /** 평균 구매 금액 */
    @Column(precision = 10, scale = 2)
    private BigDecimal avgAmount;

    // ── 체류시간 분석 ────────────────────────────
    /** 평균 체류시간 (초) */
    @Column(nullable = false)
    private Long avgDurationSeconds;

    /** 이탈 건수 (durationSeconds 10초 미만) */
    @Column(nullable = false)
    private Long bounceCount;



    // ── 메타 ─────────────────────────────────────
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
}