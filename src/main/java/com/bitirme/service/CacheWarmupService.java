package com.bitirme.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheWarmupService {

    private final CategoryService categoryService;
    private final NewsService newsService;

    /**
     * Veritabanı güncellendiğinde veya proje ilk ayağa kalktığında, 
     * Redis önbelleklerini otomatik olarak doldurur (Cache Warming).
     * Böylece ilk giren kullanıcı yavaşlık hissetmez.
     */
    public void warmupCaches() {
        log.info("🔥 Cache warming başlatıldı...");
        try {
            // Kategorileri önbelleğe al
            categoryService.getAll();
            
            // Tüm haberleri önbelleğe al (Frontend uyumluluğu için news_all)
            newsService.getAll();
            
            // Sayfalı haberlerin ilk 3 sayfasını önbelleğe al (Pagination için)
            for (int i = 0; i < 3; i++) {
                newsService.getPaginated(i, 10);
            }
            
            log.info("✅ Cache warming başarıyla tamamlandı.");
        } catch (Exception e) {
            log.error("❌ Cache warming sırasında hata oluştu: {}", e.getMessage());
        }
    }
}
