package com.liveclass.insights.repository;

import com.liveclass.insights.domain.mysql.AggregationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface AggregationResultRepository extends JpaRepository<AggregationResult, Long> {

    @Query(value = "SELECT * FROM aggregation_result WHERE agg_date = :aggDate", nativeQuery = true)
    List<AggregationResult> findByAggDate(@Param("aggDate") LocalDate aggDate);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM aggregation_result WHERE agg_date = :aggDate", nativeQuery = true)
    void deleteByAggDate(@Param("aggDate") LocalDate aggDate);

    @Query(value = "SELECT DISTINCT DATE_FORMAT(agg_date, '%Y-%m-%d') FROM aggregation_result ORDER BY DATE_FORMAT(agg_date, '%Y-%m-%d') DESC LIMIT 30", nativeQuery = true)
    List<String> findAllDistinctDates();
}
