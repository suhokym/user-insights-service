package com.liveclass.insights.domain.mysql;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Builder
@AllArgsConstructor
@Table(
        name = "aggregation_traffic_result"
)
public class AggregationTrafficHitResult {



    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //일별 집계
    @Column(name = "agg_date", nullable = false)
    private LocalDate aggDate;

    /** 집계 시간 (0~23) */
    @Column(name = "hour", nullable = false)
    private Integer hour;


    /** 전체 이벤트 발생 건수 */
    @Column(nullable = false)
    private Long totalCount;


}
