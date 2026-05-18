package com.study.userweblog.scheduler;

import com.study.userweblog.elasticsearch.ElasticsearchEventIndexer;
import com.study.userweblog.event.UserWebLogEvent;
import com.study.userweblog.event.UserWebLogEventFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "userweblog.scheduler.enabled", havingValue = "true")
public class UserWebLogScheduler {

    private final UserWebLogEventFactory eventFactory;
    private final ElasticsearchEventIndexer elasticsearchEventIndexer;

    @Value("${userweblog.scheduler.users-per-tick}")
    private int usersPerTick;

    private final AtomicLong totalGenerated = new AtomicLong(0);

    @Scheduled(fixedRateString = "${userweblog.scheduler.fixed-rate-ms:1000}")
    public void generateAndSave() {
        List<UserWebLogEvent> events = new ArrayList<>(usersPerTick);

        for (int i = 0; i < usersPerTick; i++) {
            events.add(eventFactory.create(generateUserId(), generateSessionId()));
        }

        elasticsearchEventIndexer.index(events);

        long total = totalGenerated.addAndGet(usersPerTick);
        log.info("[Scheduler] tick 완료 | 이번 tick={}건 | 누적={}건", usersPerTick, total);
    }

    private String generateUserId() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String generateSessionId() {
        return "session-" + UUID.randomUUID().toString().substring(0, 12);
    }
}
