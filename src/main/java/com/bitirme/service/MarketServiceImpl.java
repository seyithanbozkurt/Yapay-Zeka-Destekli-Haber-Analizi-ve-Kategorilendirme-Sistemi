package com.bitirme.service;

import com.bitirme.dto.market.ExternalMarketResponse;
import com.bitirme.dto.market.MarketIndicator;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class MarketServiceImpl implements MarketService {

    private static final String YAHOO_URL_TEMPLATE =
            "https://query1.finance.yahoo.com/v8/finance/chart/%s?interval=1d&range=5d";

    private static final String STOOQ_URL_TEMPLATE =
            "https://stooq.com/q/l/?s=%s&f=sd2t2ohlc&h&e=csv";

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/125.0.0.0 Safari/537.36";

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public ExternalMarketResponse getExternalIndicators() {
        MarketIndicator bist = fetchYahooIndicator("XU100.IS");
        if (bist == null) {
            bist = fetchStooqIndicator("^xu100");
        }

        MarketIndicator brent = fetchYahooIndicator("BZ=F");
        if (brent == null) {
            brent = fetchStooqIndicator("cb.f");
        }

        return new ExternalMarketResponse(bist, brent);
    }

    private MarketIndicator fetchYahooIndicator(String symbol) {
        try {
            String url = String.format(YAHOO_URL_TEMPLATE, symbol);
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
            headers.set(HttpHeaders.ACCEPT, "application/json");

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<JsonNode> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

            JsonNode body = response.getBody();
            if (body == null) return null;

            JsonNode meta = body.path("chart").path("result").path(0).path("meta");
            if (meta.isMissingNode()) {
                log.debug("Yahoo Finance meta alanı bulunamadı: {}", symbol);
                return null;
            }

            double current = meta.path("regularMarketPrice").asDouble(0d);
            if (current <= 0d) {
                log.debug("Yahoo Finance regularMarketPrice eksik: {}", symbol);
                return null;
            }

            double previous = meta.path("chartPreviousClose").asDouble(0d);
            if (previous <= 0d) previous = meta.path("previousClose").asDouble(0d);
            if (previous <= 0d) previous = meta.path("regularMarketPreviousClose").asDouble(0d);
            if (previous <= 0d) previous = current;

            double changePercent = ((current - previous) / previous) * 100.0;
            return new MarketIndicator(current, changePercent);
        } catch (RestClientException | IllegalArgumentException ex) {
            log.debug("Yahoo Finance verisi alınamadı ({}): {}", symbol, ex.getMessage());
            return null;
        }
    }

    private MarketIndicator fetchStooqIndicator(String stooqSymbol) {
        try {
            String url = String.format(STOOQ_URL_TEMPLATE, stooqSymbol);
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
            headers.set(HttpHeaders.ACCEPT, "text/csv,text/plain,*/*");

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            String body = response.getBody();
            if (body == null || body.isBlank()) return null;

            String[] lines = body.strip().split("\\r?\\n");
            if (lines.length < 2) return null;

            String[] cols = lines[1].split(",");
            if (cols.length < 7) return null;

            double open = parseDoubleSafe(cols[3]);
            double close = parseDoubleSafe(cols[6]);
            if (close <= 0d) return null;
            if (open <= 0d) open = close;

            double changePercent = ((close - open) / open) * 100.0;
            return new MarketIndicator(close, changePercent);
        } catch (RestClientException | IllegalArgumentException ex) {
            log.warn("Stooq verisi alınamadı ({}): {}", stooqSymbol, ex.getMessage());
            return null;
        }
    }

    private double parseDoubleSafe(String raw) {
        if (raw == null || raw.isBlank() || raw.equalsIgnoreCase("N/D")) return 0d;
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException ex) {
            return 0d;
        }
    }
}
