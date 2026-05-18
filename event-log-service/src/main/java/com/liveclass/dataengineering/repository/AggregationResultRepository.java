package com.liveclass.dataengineering.repository;

import com.liveclass.dataengineering.domain.mysql.AggregationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AggregationResultRepository extends JpaRepository<AggregationResult, Long> {

    List<AggregationResult> findByAggDate(LocalDate aggDate);

    @Modifying
    @Query("DELETE FROM AggregationResult r WHERE r.aggDate = :aggDate")
    void deleteByAggDate(@Param("aggDate") LocalDate aggDate);
}
