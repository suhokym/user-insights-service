package com.liveclass.dataengineering.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.ScrollResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.liveclass.dataengineering.domain.es.UserWebLogDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
     * userweblog-events 인덱스 전체 데이터 조회 (Scroll API)
     * ES max_result_window(기본 10,000) 제한 없이 전량 수집
     */
    public List<UserWebLogDocument> fetchAll() throws IOException {
        List<UserWebLogDocument> result = new ArrayList<>();

        // 1. 첫 번째 scroll 요청
        SearchResponse<UserWebLogDocument> response = elasticsearchClient.search(s -> s
                .index("userweblog-events")
                .size(SCROLL_SIZE)
                .scroll(t -> t.time(SCROLL_KEEP_ALIVE))
                .query(q -> q.matchAll(m -> m)),
                UserWebLogDocument.class
        );

        String scrollId = response.scrollId();
        List<Hit<UserWebLogDocument>> hits = response.hits().hits();

        while (!hits.isEmpty()) {
            hits.stream()
                    .map(Hit::source)
                    .collect(Collectors.toCollection(() -> result));

            log.debug("Scroll 진행 중 - 누적 건수: {}", result.size());

            // 2. 다음 페이지
            String currentScrollId = scrollId;
            ScrollResponse<UserWebLogDocument> scrollResponse = elasticsearchClient.scroll(s -> s
                    .scrollId(currentScrollId)
                    .scroll(t -> t.time(SCROLL_KEEP_ALIVE)),
                    UserWebLogDocument.class
            );

            scrollId = scrollResponse.scrollId();
            hits = scrollResponse.hits().hits();
        }

        // 3. Scroll 컨텍스트 정리
        String finalScrollId = scrollId;
        elasticsearchClient.clearScroll(c -> c.scrollId(finalScrollId));

        log.info("ES 전체 조회 완료 - 총 건수: {}", result.size());
        return result;
    }
}
