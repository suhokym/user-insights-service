package com.liveclass.dataengineering.service;

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
public class AggregationService {

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
    });

    public List<AggregationResult> run(List<UserWebLogDocument> docs, LocalDate targetDate) {
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
                        d.getDurationSeconds()
                ))
                .collect(Collectors.toList());

        // ③ DataFrame 생성
        Dataset<Row> df = sparkSession.createDataFrame(rows, SCHEMA);

        // ④ SQL로 쓰고 싶으면 뷰 등록
        df.createOrReplaceTempView("weblog");

        // ============================================


        //이상탐지
        // 1. IP별 PAGE_VIEW 건수 (60번 이상)
        Dataset<Row> suspiciousPageView = df
                .filter(col("eventType").equalTo("PAGE_VIEW"))
                .groupBy("ip")
                .agg(count("*").alias("pageViewCount"))
                .filter(col("pageViewCount").geq(60));  // 60번 이상

        // 2. IP별 PURCHASE 건수 (3번 이상)
        Dataset<Row> suspiciousPurchase = df
                .filter(col("eventType").equalTo("PURCHASE"))
                .groupBy("ip")
                .agg(count("*").alias("purchaseCount"))
                .filter(col("purchaseCount").geq(3));   // 3번 이상

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

        // 3.행동 패턴 분석
        // 5.각 유입별 체류시간 분석
        // 6.유입위치별 추적로그

        //일별 유입 위치 집계
        df.groupBy("UtmMedium").agg(count("*").alias("totalCount"));

        //유입별 구매확정 유저수 집계
        df.filter(col("eventType").equalTo("PURCHASE"))
                .groupBy("utmMedium")
                .agg(count("*").alias("purchaseCount"));



// 결과
// utmMedium    purchaseCount
// 소셜미디어      189
// 광고           252
        // 여기서부터 집계 로직 작성하면 돼요
        //
        // 사용 가능한 컬럼:
        // userId, sessionId, ip, eventType,
        // uri, targetId, amount, utmMedium, durationSeconds
        //
        // eventType 값:
        // "PAGE_VIEW", "PAGE_LEAVE", "CLICK", "PURCHASE"
        //
        // utmMedium 값:
        // "광고", "소셜미디어", "웹검색", "이메일", "배너광고", null(직접유입)
        //
        // 집계 예시:
        // df.groupBy("utmMedium").agg(count("*").alias("totalCount"))
        //
        // SQL 예시:
        // sparkSession.sql("SELECT utmMedium, COUNT(*) FROM weblog GROUP BY utmMedium")
        // ============================================

        Dataset<Row> aggregated = null; // ← 여기에 집계 결과 넣기

        // ⑤ Row → AggregationResult 변환
        List<AggregationResult> results = aggregated
                .map((MapFunction<Row, AggregationResult>) row ->
                                AggregationResult.builder()
                                        .aggDate(targetDate)
                                        .utmMedium(row.getAs("utmMedium"))
                                        // ← 집계한 컬럼들 여기에 추가
                                        .build(),
                        Encoders.bean(AggregationResult.class)
                )
                .collectAsList();

        log.info("Spark 집계 완료 - 결과 건수: {}", results.size());
        return results;
    }
}