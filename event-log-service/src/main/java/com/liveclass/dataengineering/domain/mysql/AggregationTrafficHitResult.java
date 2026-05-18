package com.liveclass.dataengineering.domain.mysql;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "aggregation_traffic_result"
)
public class AggregationTrafficHitResult {



    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //일별 집계
    @Column(name = "agg_date", nullable = false)
    private LocalDate agg_date;

    @Column(name = "agg_time", nullable = false)
    private LocalTime aggTime;

    /** 시간별 트래픽 히트 건수 */
    private Long trafficCount;

    /** 전체 이벤트 발생 건수 */
    @Column(nullable = false)
    private Long totalCount;


}
