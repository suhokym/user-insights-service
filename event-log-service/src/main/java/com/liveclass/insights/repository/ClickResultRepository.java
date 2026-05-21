package com.liveclass.insights.repository;

import com.liveclass.insights.domain.mysql.AggregationClickResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ClickResultRepository
        extends JpaRepository<AggregationClickResult, Long> {

    List<AggregationClickResult> findByAggDateOrderByClickCountDesc(LocalDate aggDate);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM aggregation_click_result WHERE agg_date = :aggDate", nativeQuery = true)
    void deleteByAggDate(@Param("aggDate") LocalDate aggDate);
}
