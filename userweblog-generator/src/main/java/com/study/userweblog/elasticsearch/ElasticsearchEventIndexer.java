package com.study.userweblog.elasticsearch;

import com.study.userweblog.event.UserWebLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchEventIndexer {

    private static final BulkOptions BULK_OPTIONS = BulkOptions.builder()
            .withRefreshPolicy(RefreshPolicy.NONE)
            .build();

    private final ElasticsearchOperations operations;

    public void index(List<UserWebLogEvent> events) {
        List<IndexQuery> queries = events.stream()
                .map(e -> new IndexQueryBuilder()
                        .withId(UUID.randomUUID().toString())
                        .withObject(toDocument(e))
                        .build())
                .toList();
        try {
            operations.bulkIndex(queries, BULK_OPTIONS, UserWebLogDocument.class);
            log.debug("[ES] {}건 인덱싱 완료", queries.size());
        } catch (Exception e) {
            log.warn("[ES] 인덱싱 실패: {}", e.getMessage());
        }
    }

    public void forceRefresh() {
        try {
            operations.indexOps(UserWebLogDocument.class).refresh();
            log.info("[ES] refresh 완료");
        } catch (Exception e) {
            log.warn("[ES] refresh 실패: {}", e.getMessage());
        }
    }

    private UserWebLogDocument toDocument(UserWebLogEvent e) {
        return UserWebLogDocument.builder()
                .id(UUID.randomUUID().toString())
                .userId(e.getUserId())
                .sessionId(e.getSessionId())
                .ip(e.getIp())
                .eventType(e.getEventType().name())
                .uri(e.getUri())
                .targetId(e.getTargetId())
                .amount(e.getAmount())
                .utmMedium(e.getUtmMedium())
                .durationSeconds(e.getDurationSeconds())
                .occurredAt(e.getOccurredAt())
                .build();
    }
}
