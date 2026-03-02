package com.bitirme.nlp.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;
import java.util.Optional;

/**
 * SparkSession bean (local mode). ML pipeline bu session ile çalışır.
 */
@Configuration
@Slf4j
public class SparkConfig {

    private SparkSession sparkSession;

    @Bean
    public SparkSession sparkSession(MlClassifierProperties properties) {
        SparkConf conf = new SparkConf()
                .setAppName("NewsClassifier")
                .setMaster(properties.getSparkMaster())
                .set("spark.driver.host", "localhost")
                .set("spark.ui.enabled", "false");
        sparkSession = SparkSession.builder()
                .config(conf)
                .getOrCreate();
        sparkSession.sparkContext().setLogLevel("WARN");
        log.info("SparkSession created with master: {}", properties.getSparkMaster());
        return sparkSession;
    }

    @PreDestroy
    public void close() {
        if (sparkSession != null) {
            sparkSession.close();
            log.info("SparkSession closed.");
        }
    }
}
