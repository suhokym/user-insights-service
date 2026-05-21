package com.liveclass.insights.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.ScrollResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.liveclass.insights.domain.es.UserWebLogDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
public class EsReaderService {

    private final ElasticsearchClient elasticsearchClient;

    private static final int SCROLL_SIZE = 1_000;
    private static final String SCROLL_KEEP_ALIVE = "2m";

    /**
     * 특정 날짜의 데이터만 조회 (Scroll API)
     */
    public List<UserWebLogDocument> fetchByDate(LocalDate targetDate) throws IOException {
        List<UserWebLogDocument> result = new ArrayList<>();

        // 1. 첫 번째 scroll 요청 (날짜 범위 필터 추가)
        SearchResponse<UserWebLogDocument> response = elasticsearchClient.search(s -> s
                        .index("userweblog-events")
                        .size(SCROLL_SIZE)
                        .scroll(t -> t.time(SCROLL_KEEP_ALIVE))
                        .query(q -> q
                                .range(r -> r
                                        .date(d -> d
                                                .field("occurredAt")
                                                .gte(targetDate.atStartOfDay().toString())
                                                .lt(targetDate.plusDays(1).atStartOfDay().toString())
                                        )
                                )
                        ),
                UserWebLogDocument.class
        );

        String scrollId = response.scrollId();
        List<Hit<UserWebLogDocument>> hits = response.hits().hits();

        while (!hits.isEmpty()) {
            hits.stream()
                    .map(Hit::source)
                    .collect(Collectors.toCollection(() -> result));

            log.debug("Scroll 진행 중 - date: {} 누적 건수: {}", targetDate, result.size());

            String currentScrollId = scrollId;
            ScrollResponse<UserWebLogDocument> scrollResponse = elasticsearchClient.scroll(s -> s
                            .scrollId(currentScrollId)
                            .scroll(t -> t.time(SCROLL_KEEP_ALIVE)),
                    UserWebLogDocument.class
            );

            scrollId = scrollResponse.scrollId();
            hits = scrollResponse.hits().hits();
        }

        String finalScrollId = scrollId;
        elasticsearchClient.clearScroll(c -> c.scrollId(finalScrollId));

        log.info("ES 날짜별 조회 완료 - date: {} 총 건수: {}", targetDate, result.size());
        return result;
    }
}