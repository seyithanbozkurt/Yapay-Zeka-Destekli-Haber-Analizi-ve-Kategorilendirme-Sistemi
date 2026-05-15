package com.bitirme.controller;

import com.bitirme.dto.admin.CacheKeysResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Cache + Redis ile yazılan anahtarları incelemek için (redis-cli / GUI yerine API).
 */
@RestController
@RequestMapping("/api/admin/cache")
@RequiredArgsConstructor
@Tag(name = "Önbellek (Redis)", description = "Redis anahtarlarını listeleme — yalnızca ADMIN")
public class CacheAdminController {

    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/keys")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Redis anahtarlarını listele",
            description = "SCAN ile desene uyan anahtarları döner (ör. `categories_all::*`, `*news*`). "
                    + "Spring `@Cacheable` değerleri genelde `cacheAdı::anahtar` biçimindedir. "
                    + "Çağrı için **ADMIN** rolü ve geçerli JWT gerekir (Authorize).")
    public ResponseEntity<CacheKeysResponse> listKeys(
            @Parameter(description = "Glob benzeri desen; örn. `*`, `news_all::*`, `*classification*`")
            @RequestParam(defaultValue = "*") String pattern,
            @Parameter(description = "En fazla döndürülecek anahtar sayısı (1–10000)")
            @RequestParam(defaultValue = "2000") int limit) {
        int cap = Math.min(Math.max(limit, 1), 10_000);
        String pat = (pattern == null || pattern.isBlank()) ? "*" : pattern;
        try {
            List<String> keys = scanKeys(pat, cap);
            return ResponseEntity.ok(CacheKeysResponse.ok(keys.size(), cap, pat, keys));
        } catch (DataAccessException e) {
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(CacheKeysResponse.fail(cap, pat, "Redis erişilemedi: " + e.getMessage()));
        }
    }

    private List<String> scanKeys(String pattern, int limit) {
        java.util.Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null) {
            return new ArrayList<>();
        }
        List<String> out = new ArrayList<>(keys);
        if (out.size() > limit) {
            return out.subList(0, limit);
        }
        return out;
    }
}
