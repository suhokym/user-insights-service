package com.liveclass.dataengineering.service;

import com.liveclass.dataengineering.domain.es.UserWebLogDocument;
import com.liveclass.dataengineering.domain.mysql.AggregationResult;
import com.liveclass.dataengineering.job.AggregationJob;
import com.liveclass.dataengineering.repository.AggregationResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AggregationService {

    private final EsReaderService esReaderService;
    private final AggregationJob aggregationJob;
    private final AggregationResultRepository aggregationResultRepository;

    /**
     * 특정 날짜 기준으로 ES → Spark 집계 → MySQL 저장
     * 멱등성 보장: 같은 날짜 재실행 시 기존 데이터 삭제 후 재적재
     */
    @Transactional
    public void aggregate(LocalDate targetDate) {
        log.info("집계 파이프라인 시작 - date: {}", targetDate);

        // 1. ES 전체 데이터 조회
        List<UserWebLogDocument> rawDocs;
        try {
            rawDocs = esReaderService.fetchAll();
        } catch (Exception e) {
            log.error("ES 조회 실패 - date: {}", targetDate, e);
            throw new RuntimeException("ES 조회 실패", e);
        }

        if (rawDocs.isEmpty()) {
            log.warn("집계 대상 데이터 없음 - date: {}", targetDate);
            return;
        }

        // 2. Spark로 집계
        List<AggregationResult> results = aggregationJob.run(rawDocs, targetDate);

        // 3. 기존 데이터 삭제 (재실행 멱등성)
        aggregationResultRepository.deleteByAggDate(targetDate);

        // 4. MySQL 저장
        aggregationResultRepository.saveAll(results);

        log.info("집계 파이프라인 완료 - date: {}, 저장 건수: {}", targetDate, results.size());
    }

    /**
     * 어제 날짜 기준 집계 (스케줄러용)
     */
    public void aggregateYesterday() {
        aggregate(LocalDate.now().minusDays(1));
    }
}
