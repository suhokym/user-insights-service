package com.liveclass.insights.job;

import com.liveclass.insights.domain.es.UserWebLogDocument;
import com.liveclass.insights.domain.mysql.AggregationClickResult;
import com.liveclass.insights.domain.mysql.AggregationResult;
import com.liveclass.insights.domain.mysql.AggregationTrafficHitResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.spark.sql.functions.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationJob {

    private final SparkSession sparkSession;

    // ① 스키마 정의 - ES 필드랑 순서/타입 반드시 일치해야 함
    private static final StructType SCHEMA = DataTypes.createStructType(new StructField[]{
            DataTypes.createStructField("userId",          DataTypes.StringType, true),
            DataTypes.createStructField("sessionId",       DataTypes.StringType, true),
            DataTypes.createStructField("ip",              DataTypes.StringType, true),
            DataTypes.createStructField("eventType",       DataTypes.StringType, true),
            DataTypes.createStructField("uri",             DataTypes.StringType, true),
            DataTypes.createStructField("targetId",        DataTypes.StringType, true),
            DataTypes.createStructField("amount",          DataTypes.LongType,   true),
            DataTypes.createStructField("utmMedium",       DataTypes.StringType, true),
            DataTypes.createStructField("durationSeconds", DataTypes.LongType,   true),
            DataTypes.createStructField("occurredAt",      DataTypes.StringType, true),
    });

    public AggregationJobResult run(List<UserWebLogDocument> docs, LocalDate targetDate) {
        log.info("Spark 집계 시작 - date: {}, 입력 건수: {}", targetDate, docs.size());




        // ② ES 문서 → Row 변환 (SCHEMA 순서랑 반드시 일치)
        List<Row> rows = docs.stream()
                .map(d -> RowFactory.create(
                        d.getUserId(),
                        d.getSessionId(),
                        d.getIp(),
                        d.getEventType(),
                        d.getUri(),
                        d.getTargetId(),
                        d.getAmount(),
                        d.getUtmMedium(),
                        d.getDurationSeconds(),
                        d.getOccurredAt() != null ? d.getOccurredAt().toString() : null
                ))
                .collect(Collectors.toList());

        // ③ DataFrame 생성
        Dataset<Row> df = sparkSession.createDataFrame(rows, SCHEMA)
                .withColumn("utmMedium",
                        when(col("utmMedium").isNull(), "직접유입").otherwise(col("utmMedium")));

        // ④ SQL로 쓰고 싶으면 뷰 등록
        df.createOrReplaceTempView("weblog");

        // ============================================


        //이상탐지
        // 1. IP별 PAGE_VIEW 건수 (1000번 이상)
        Dataset<Row> suspiciousPageView = df
                .filter(col("eventType").equalTo("PAGE_VIEW"))
                .groupBy("ip")
                .agg(count("*").alias("pageViewCount"))
                .filter(col("pageViewCount").geq(1000));

        // 2. IP별 PURCHASE 건수 (50번 이상)
        Dataset<Row> suspiciousPurchase = df
                .filter(col("eventType").equalTo("PURCHASE"))
                .groupBy("ip")
                .agg(count("*").alias("purchaseCount"))
                .filter(col("purchaseCount").geq(50));

        // 3. 둘 다 해당하는 IP = 봇
        Dataset<Row> botIps = suspiciousPageView
                .join(suspiciousPurchase, "ip")  // inner join = 둘 다 해당하는 IP만
                .select("ip", "pageViewCount", "purchaseCount");

        botIps.show();
        // +---------+-------------+-------------+
        // |       ip|pageViewCount|purchaseCount|
        // +---------+-------------+-------------+
        // |1.2.3.4  |          180|            9|  ← 봇!
        // |5.6.7.8  |           60|            3|  ← 봇!

        // 4. 봇 IP 목록 추출
        List<String> botIpList = botIps
                .select("ip")
                .collectAsList()
                .stream()
                .map(row -> row.<String>getAs("ip"))
                .collect(Collectors.toList());

        log.warn("봇 IP 탐지 {}개: {}", botIpList.size(), botIpList);

        // 5. 봇 제거한 클린 데이터
        Dataset<Row> cleanDf = botIpList.isEmpty()
                ? df
                : df.filter(not(col("ip").isin(botIpList.toArray(new String[0]))));

        // 행동 패턴 분석
        Dataset<Row> clickPattern = cleanDf
                .filter(col("eventType").equalTo("CLICK"))
                .groupBy("utmMedium", "targetId")
                .agg(count("*").alias("clickCount"))
                .orderBy(col("clickCount").desc());

        // 일별 유입 위치 집계
        Dataset<Row> trafficDf = cleanDf
                .groupBy("utmMedium")
                .agg(count("*").alias("totalCount"));

        // 유입별 구매 확정 유저수 집계
        Dataset<Row> purchaseDf = cleanDf
                .filter(col("eventType").equalTo("PURCHASE"))
                .groupBy("utmMedium")
                .agg(count("*").alias("purchaseCount"),
                        sum("amount").alias("totalAmount"),
                        avg("amount").alias("avgAmount"));

        // 각 유입별 체류시간 분석
        Dataset<Row> durationDf = cleanDf
                .filter(col("eventType").equalTo("PAGE_LEAVE"))
                .groupBy("utmMedium")
                .agg(
                        avg("durationSeconds").alias("avgDurationSeconds"),
                        sum(when(col("durationSeconds").lt(10), 1).otherwise(0)).alias("bounceCount")
                );

        //시간별 전체 트래픽 집계
        Dataset<Row> hourlyTraffic = cleanDf.withColumn("hour", hour(to_timestamp(col("occurredAt"))))
                .groupBy("hour")
                .agg(count("*").alias("totalCount"))
                .orderBy("hour");



        // utmMedium 기준으로 전체 JOIN
        Dataset<Row> aggregated = trafficDf
                .join(purchaseDf, "utmMedium", "left")
                .join(durationDf, "utmMedium", "left")
                .withColumn("conversionRate",                          // ← 추가
                round(
                        col("purchaseCount").divide(col("totalCount")).multiply(100), 2
                ));

        // Row → AggregationResult 변환
        List<AggregationResult> aggregationResults = aggregated
                .map((MapFunction<Row, AggregationResult>) row -> {
                    return AggregationResult.builder()
                            .aggDate(targetDate)
                            .utmMedium(row.<String>getAs("utmMedium") != null ? row.getAs("utmMedium") : "직접유입")
                            .totalCount(row.<Long>getAs("totalCount"))
                            .purchaseCount(row.<Long>getAs("purchaseCount") != null
                                    ? row.<Long>getAs("purchaseCount") : 0L)
                            .totalAmount(row.<Long>getAs("totalAmount") != null
                                    ? row.<Long>getAs("totalAmount") : 0L)
                            .avgDurationSeconds(row.<Double>getAs("avgDurationSeconds") != null
                                    ? row.<Double>getAs("avgDurationSeconds").longValue() : 0L)
                            .bounceCount(row.<Long>getAs("bounceCount") != null
                                    ? row.<Long>getAs("bounceCount") : 0L)
                            .conversionRate(row.<Double>getAs("conversionRate") != null
                                    ? BigDecimal.valueOf(row.<Double>getAs("conversionRate")) : BigDecimal.ZERO)
                            .avgAmount(row.<Double>getAs("avgAmount") != null
                                    ? BigDecimal.valueOf(row.<Double>getAs("avgAmount")) : BigDecimal.ZERO)
                            .build();
                }, Encoders.bean(AggregationResult.class))
                .collectAsList();

        List<AggregationTrafficHitResult> trafficHitResults =
                hourlyTraffic.map((MapFunction<Row, AggregationTrafficHitResult>) row ->
                        AggregationTrafficHitResult
                                .builder()
                                .aggDate(targetDate)
                                .hour(row.<Integer>getAs("hour"))
                                .totalCount(row.getAs("totalCount"))
                                .build(), Encoders.bean(AggregationTrafficHitResult.class)).collectAsList();

        List<AggregationClickResult> clickResults = clickPattern
                .map((MapFunction<Row, AggregationClickResult>) row -> {
                    return AggregationClickResult.builder()
                            .aggDate(targetDate)
                            .utmMedium(row.<String>getAs("utmMedium"))
                            .targetId(row.<String>getAs("targetId"))
                            .clickCount(row.<Long>getAs("clickCount"))
                            .build();
                }, Encoders.bean(AggregationClickResult.class))
                .collectAsList();

        AggregationJobResult result = AggregationJobResult.builder()
                .aggregationResults(aggregationResults)
                .trafficHitResults(trafficHitResults)
                .clickResults(clickResults)
                .build();

        log.info("Spark 집계 완료 - aggregation: {}건 | traffic: {}건 | click: {}건",
                aggregationResults.size(),
                trafficHitResults.size(),
                clickResults.size());
        return result;
    }
}