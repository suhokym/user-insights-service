package com.liveclass.insights.config;

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

    @Bean(destroyMethod = "close")
    public SparkSession sparkSession() {
        System.setProperty("io.netty.tryReflectionSetAccessible", "true");
        System.setProperty("hadoop.home.dir", "/");

        return SparkSession.builder()
                .appName(appName)
                .master(master)
                .config("spark.sql.session.timeZone", "Asia/Seoul")
                .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .config("spark.ui.enabled", "false")
                .config("spark.driver.extraJavaOptions",
                        "--add-exports java.base/sun.nio.ch=ALL-UNNAMED " +
                                "--add-opens java.base/sun.nio.ch=ALL-UNNAMED " +
                                "--add-opens java.base/java.lang=ALL-UNNAMED " +
                                "--add-opens java.base/java.nio=ALL-UNNAMED " +
                                "--add-opens java.base/java.util=ALL-UNNAMED")
                .getOrCreate();
    }
}
