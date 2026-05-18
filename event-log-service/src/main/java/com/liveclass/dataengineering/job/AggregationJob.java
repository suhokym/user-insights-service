package com.liveclass.dataengineering.job;

import com.liveclass.dataengineering.domain.es.UserWebLogDocument;
import com.liveclass.dataengineering.domain.mysql.AggregationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.spark.sql.functions.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationJob {

    private final SparkSession sparkSession;

    private static final StructType SCHEMA = DataTypes.createStructType(new StructField[]{
            DataTypes.createStructField("userId",          DataTypes.StringType, true),
            DataTypes.createStructField("sessionId",       DataTypes.StringType, true),
            DataTypes.createStructField("eventType",       DataTypes.StringType, true),
            DataTypes.createStructField("uri",             DataTypes.StringType, true),
            DataTypes.createStructField("amount",          DataTypes.LongType,   true),
            DataTypes.createStructField("utmMedium",       DataTypes.StringType, true),
            DataTypes.createStructField("durationSeconds", DataTypes.LongType,   true),
    });

    /**
     * 집계 기준: eventType + utmMedium
     * 메트릭: totalCount, uniqueUsers, uniqueSessions, totalAmount, totalDurationSeconds
     */
    public List<AggregationResult> run(List<UserWebLogDocument> docs, LocalDate targetDate) {
        log.info("Spark 집계 시작 - date: {}, 입력 건수: {}", targetDate, docs.size());

        List<Row> rows = docs.stream()
                .map(d -> RowFactory.create(
                        d.getUserId(),
                        d.getSessionId(),
                        d.getEventType(),
                        d.getUri(),
                        d.getAmount(),
                        d.getUtmMedium(),
                        d.getDurationSeconds()
                ))
                .collect(Collectors.toList());

        Dataset<Row> df = sparkSession.createDataFrame(rows, SCHEMA);

        Dataset<Row> aggregated = df
                .groupBy(col("eventType"), col("utmMedium"))
                .agg(
                        count("*").alias("totalCount"),
                        countDistinct("userId").alias("uniqueUsers"),
                        countDistinct("sessionId").alias("uniqueSessions"),
                        sum(coalesce(col("amount"), lit(0L))).alias("totalAmount"),
                        sum(coalesce(col("durationSeconds"), lit(0L))).alias("totalDurationSeconds")
                );

        List<AggregationResult> results = aggregated
                .map((MapFunction<Row, AggregationResult>) row ->
                        AggregationResult.builder()
                                .aggDate(targetDate)
                                .eventType(row.getAs("eventType"))
                                .utmMedium(row.getAs("utmMedium"))
                                .totalCount(row.getAs("totalCount"))
                                .uniqueUsers(row.getAs("uniqueUsers"))
                                .uniqueSessions(row.getAs("uniqueSessions"))
                                .totalAmount(row.<Long>getAs("totalAmount") != null ? row.getAs("totalAmount") : 0L)
                                .totalDurationSeconds(row.<Long>getAs("totalDurationSeconds") != null ? row.getAs("totalDurationSeconds") : 0L)
                                .build(),
                        Encoders.bean(AggregationResult.class)
                )
                .collectAsList();

        log.info("Spark 집계 완료 - 결과 건수: {}", results.size());
        return results;
    }
}
