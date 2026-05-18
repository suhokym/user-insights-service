package com.study.userweblog.bulk;

import com.study.userweblog.elasticsearch.ElasticsearchEventIndexer;
import com.study.userweblog.event.UserSessionFactory;
import com.study.userweblog.event.UserWebLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class BulkEventGenerator implements ApplicationRunner {

    private static final Random RANDOM = new Random();

    private final UserSessionFactory sessionFactory;
    private final ElasticsearchEventIndexer elasticsearchEventIndexer;

    @Value("${userweblog.bulk.total:100000}")
    private int total;

    @Value("${userweblog.bulk.batch-size:5000}")
    private int batchSize;

    @Value("${userweblog.bulk.days:7}")
    private int days;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[Bulk] 목표 {}건 생성 시작 ({}일 분산)", total, days);
        long start = System.currentTimeMillis();

        List<UserWebLogEvent> batch = new ArrayList<>(batchSize);
        int saved = 0;

        while (saved < total) {
            List<UserWebLogEvent> session = sessionFactory.createSession(randomTimeInPastDays(days));
            batch.addAll(session);

            if (batch.size() >= batchSize) {
                elasticsearchEventIndexer.index(batch);
                saved += batch.size();
                batch.clear();
                log.info("[Bulk] 진행: {}건 저장 완료", saved);
            }
        }

        if (!batch.isEmpty()) {
            elasticsearchEventIndexer.index(batch);
            saved += batch.size();
        }

        elasticsearchEventIndexer.forceRefresh();

        long elapsed = System.currentTimeMillis() - start;
        log.info("[Bulk] 완료 — {}건, 소요시간: {}ms", saved, elapsed);
    }

    private LocalDateTime randomTimeInPastDays(int days) {
        long totalSeconds = (long) days * 24 * 60 * 60;
        long offset = (long) (RANDOM.nextDouble() * totalSeconds);
        return LocalDateTime.now().minusSeconds(offset);
    }
}
