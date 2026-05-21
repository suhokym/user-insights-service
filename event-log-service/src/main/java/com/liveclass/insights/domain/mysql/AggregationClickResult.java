package com.liveclass.insights.domain.mysql;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "aggregation_click_result",
        uniqueConstraints = @UniqueConstraint(columnNames = {"agg_date", "utm_medium", "target_id"})
)
public class AggregationClickResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 집계 날짜 */
    @Column(name = "agg_date", nullable = false)
    private LocalDate aggDate;

    /** 유입 채널 */
    @Column(name = "utm_medium", length = 100)
    private String utmMedium;

    /** 클릭된 버튼/요소 */
    @Column(name = "target_id", length = 200)
    private String targetId;

    /** 클릭 건수 */
    @Column(nullable = false)
    private Long clickCount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}