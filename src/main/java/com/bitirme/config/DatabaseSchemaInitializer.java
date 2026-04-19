package com.bitirme.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("ALTER TABLE news ALTER COLUMN content TYPE TEXT");
            log.info("Database schema verified: news.content is TEXT.");
        } catch (Exception e) {
            log.warn("Could not alter news.content column to TEXT: {}", e.getMessage());
        }
    }
}
