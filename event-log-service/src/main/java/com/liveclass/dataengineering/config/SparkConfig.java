package com.liveclass.dataengineering.config;

import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SparkConfig {

    @Value("${spark.app-name}")
    private String appName;

    @Value("${spark.master}")
    private String master;

    @Value("${spark.log-level:WARN}")
    private String logLevel;

    @Bean(destroyMethod = "close")
    public SparkSession sparkSession() {
        SparkSession session = SparkSession.builder()
                .appName(appName)
                .master(master)
                .config("spark.sql.session.timeZone", "Asia/Seoul")
                .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .config("spark.ui.enabled", "false")
                .getOrCreate();

        session.sparkContext().setLogLevel(logLevel);
        return session;
    }
}
