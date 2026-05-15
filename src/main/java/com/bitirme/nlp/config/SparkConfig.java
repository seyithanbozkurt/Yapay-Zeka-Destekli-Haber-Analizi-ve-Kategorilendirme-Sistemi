package com.bitirme.nlp.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JavaSparkContext bean (local mode). RDD tabanlı Spark MLlib bu context ile çalışır.
 */
@Configuration
@ConditionalOnProperty(prefix = "ml.classifier", name = "enabled", havingValue = "true")
@Slf4j
public class SparkConfig {

    @Lazy
    @Bean(destroyMethod = "close")
    public JavaSparkContext sparkContext(MlClassifierProperties properties) {
        // Not: SecurityManager'ı runtime'da kurmak (System.setSecurityManager) bazı kütüphanelerde
        // izin hatalarına sebep olabiliyor. Spark için gereken "allow" davranışı JVM açılış argümanında
        // (-Djava.security.manager=allow) sağlanmalı.
        log.info(
                "Spark init - java.security.manager property: {}, SecurityManager active: {}",
                System.getProperty("java.security.manager"),
                System.getSecurityManager() != null
        );
        SparkConf conf = new SparkConf()
                .setAppName("NewsClassifier")
                .setMaster(properties.getSparkMaster())
                // Bazı ortamlarda (özellikle container/sandbox) `localhost` DNS/IP çözümlemesi patlayabiliyor.
                // Spark'ın local mode içinde ağ adresine ihtiyaç duymasını `127.0.0.1` ile güvenli hale getiriyoruz.
                .set("spark.driver.host", "127.0.0.1")
                .set("spark.driver.bindAddress", "127.0.0.1")
                .set("spark.local.ip", "127.0.0.1")
                .set("spark.local.hostname", "127.0.0.1")
                .set("spark.ui.enabled", "false")
                .set("spark.default.parallelism", "2")
                .set("spark.sql.shuffle.partitions", "2")
                .set("spark.driver.allowMultipleContexts", "false")
                .set("spark.hadoop.fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem")
                // JRE Docker: lz4 codec sık "not available"; lzf/snappy daha güvenli
                .set("spark.io.compression.codec", properties.getSparkIoCompressionCodec())
                .set("spark.shuffle.spill.compress", "true")
                .set("spark.shuffle.compress", "true");
        JavaSparkContext ctx = new JavaSparkContext(conf);
        ctx.setLogLevel("WARN");
        log.info("JavaSparkContext created with master: {}", properties.getSparkMaster());
        return ctx;
    }
}
