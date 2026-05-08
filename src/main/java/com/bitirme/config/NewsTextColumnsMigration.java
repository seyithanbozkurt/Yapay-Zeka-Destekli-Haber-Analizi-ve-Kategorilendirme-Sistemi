package com.bitirme.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Eski şemada {@code news.content} / {@code news.title} PostgreSQL'de {@code bytea} kalmışsa
 * {@code LOWER(...)} arama sorguları patlar. Kolonları {@code text} yapar.
 * <p>
 * Not: {@code current_schema()} bazen {@code public} değildir; tablolar genelde {@code public}
 * şemasında olduğu için kontrol ve {@code ALTER} hep {@code public.news} üzerinden yapılır.
 */
@Component
public class NewsTextColumnsMigration {

    private static final Logger log = LoggerFactory.getLogger(NewsTextColumnsMigration.class);
    private static final String SCHEMA = "public";

    private final JdbcTemplate jdbcTemplate;

    public NewsTextColumnsMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        migrateIfBytea("content");
        migrateIfBytea("title");
    }

    private void migrateIfBytea(String column) {
        try {
            if (!tableExists("news")) {
                log.debug("{}.news tablosu yok, bytea migrasyonu atlanıyor", SCHEMA);
                return;
            }
            String udt = columnUdtName("news", column);
            if (udt == null) {
                log.debug("news.{} kolonu bulunamadı, atlanıyor", column);
                return;
            }
            if (!"bytea".equalsIgnoreCase(udt)) {
                return;
            }
            log.warn("{}.news.{} kolonu bytea; arama için text'e dönüştürülüyor", SCHEMA, column);
            String sql = "ALTER TABLE " + SCHEMA + ".news ALTER COLUMN " + column + " TYPE text USING "
                    + "CASE WHEN " + column + " IS NULL THEN NULL "
                    + "ELSE convert_from(" + column + ", 'UTF8') END";
            jdbcTemplate.execute(sql);
            log.info("{}.news.{} kolonu text olarak güncellendi", SCHEMA, column);
        } catch (Exception e) {
            log.error("{}.news.{} kolonu dönüştürülemedi: {}", SCHEMA, column, e.getMessage(), e);
        }
    }

    private boolean tableExists(String table) {
        Boolean exists = jdbcTemplate.query(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables "
                        + "WHERE table_schema = ? AND table_name = ?)",
                ps -> {
                    ps.setString(1, SCHEMA);
                    ps.setString(2, table);
                },
                rs -> rs.next() && rs.getBoolean(1));
        return Boolean.TRUE.equals(exists);
    }

    private String columnUdtName(String table, String column) {
        List<String> rows = jdbcTemplate.query(
                "SELECT udt_name FROM information_schema.columns "
                        + "WHERE table_schema = ? AND table_name = ? AND column_name = ?",
                ps -> {
                    ps.setString(1, SCHEMA);
                    ps.setString(2, table);
                    ps.setString(3, column);
                },
                (rs, rowNum) -> rs.getString("udt_name"));
        return rows.isEmpty() ? null : rows.get(0);
    }
}
