package com.liveclass.insights.job;

import com.liveclass.insights.domain.mysql.AggregationClickResult;
import com.liveclass.insights.domain.mysql.AggregationResult;
import com.liveclass.insights.domain.mysql.AggregationTrafficHitResult;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AggregationJobResult {

    /** 유입 + 구매 + 체류시간 집계 */
    private List<AggregationResult> aggregationResults;

    /** 시간별 트래픽 집계 */
    private List<AggregationTrafficHitResult> trafficHitResults;

    /** 행동 패턴 (클릭) 집계 */
    private List<AggregationClickResult> clickResults;
}
