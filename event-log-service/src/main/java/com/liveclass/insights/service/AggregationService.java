package com.liveclass.insights.service;

import com.liveclass.insights.domain.es.UserWebLogDocument;

import com.liveclass.insights.job.AggregationJob;
import com.liveclass.insights.job.AggregationJobResult;
import com.liveclass.insights.repository.AggregationResultRepository;
import com.liveclass.insights.repository.ClickResultRepository;
import com.liveclass.insights.repository.TrafficHitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AggregationService {

    private final EsReaderService esReaderService;
    private final AggregationJob aggregationJob;
    private final AggregationResultRepository aggregationResultRepository;
    private final TrafficHitRepository trafficHitResultRepository;
    private final ClickResultRepository clickResultRepository;

    public void aggregateWeek() {
        LocalDate end   = LocalDate.now().minusDays(1);
        LocalDate start = end.minusDays(6);

        log.info("1주일 집계 시작 - {} ~ {}", start, end);

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            try {
                List<UserWebLogDocument> docs = esReaderService.fetchByDate(date);

                if (docs.isEmpty()) {
                    log.warn("집계 대상 데이터 없음 - date: {}", date);
                    continue;
                }

                log.info("집계 중 - {} ({}건)", date, docs.size());
                aggregateByDate(date, docs);

            } catch (IOException e) {
                log.error("ES 조회 실패 - date: {}", date, e);
                throw new RuntimeException("ES 조회 실패 - date: " + date, e);
            }
        }

        log.info("1주일 집계 완료 - {} ~ {}", start, end);
    }

    @Transactional
    public void aggregateByDate(LocalDate targetDate, List<UserWebLogDocument> docs) {
        AggregationJobResult jobResult = aggregationJob.run(docs, targetDate);

        // 멱등성
        aggregationResultRepository.deleteByAggDate(targetDate);
        trafficHitResultRepository.deleteByAggDate(targetDate);
        clickResultRepository.deleteByAggDate(targetDate);

        // 저장
        aggregationResultRepository.saveAll(jobResult.getAggregationResults());
        trafficHitResultRepository.saveAll(jobResult.getTrafficHitResults());
        clickResultRepository.saveAll(jobResult.getClickResults());

        log.info("집계 저장 완료 - date: {}", targetDate);
    }
}