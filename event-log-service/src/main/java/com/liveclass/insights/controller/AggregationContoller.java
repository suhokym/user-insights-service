package com.liveclass.insights.controller;

import com.liveclass.insights.repository.AggregationResultRepository;
import com.liveclass.insights.repository.ClickResultRepository;
import com.liveclass.insights.repository.TrafficHitRepository;
import com.liveclass.insights.service.AggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/aggregation")
public class AggregationContoller {

    private final AggregationService aggregationService;
    private final AggregationResultRepository aggregationResultRepository;
    private final TrafficHitRepository trafficHitRepository;
    private final ClickResultRepository clickResultRepository;

    @PostMapping("/week")
    public ResponseEntity<String> aggregationWeek() {
        aggregationService.aggregateWeek();
        return ResponseEntity.ok("1주일치 집계 완료");
    }

    @GetMapping("/dates")
    public ResponseEntity<List<String>> getAvailableDates() {
        return ResponseEntity.ok(aggregationResultRepository.findAllDistinctDates());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard(@RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        return ResponseEntity.ok(Map.of(
                "aggregation", aggregationResultRepository.findByAggDate(localDate),
                "traffic",     trafficHitRepository.findByAggDateOrderByHourAsc(localDate),
                "clicks",      clickResultRepository.findByAggDateOrderByClickCountDesc(localDate)
        ));
    }
}
