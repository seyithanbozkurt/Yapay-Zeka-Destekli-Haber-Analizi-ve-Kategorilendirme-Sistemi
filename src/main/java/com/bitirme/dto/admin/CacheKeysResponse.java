package com.bitirme.dto.admin;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Redis SCAN ile dönen anahtar listesi (Spring {@code @Cacheable} anahtarları dahil).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CacheKeysResponse(
        int returned,
        int limit,
        String pattern,
        List<String> keys,
        String error
) {
    public static CacheKeysResponse ok(int returned, int limit, String pattern, List<String> keys) {
        return new CacheKeysResponse(returned, limit, pattern, keys, null);
    }

    public static CacheKeysResponse fail(int limit, String pattern, String error) {
        return new CacheKeysResponse(0, limit, pattern, List.of(), error);
    }
}
