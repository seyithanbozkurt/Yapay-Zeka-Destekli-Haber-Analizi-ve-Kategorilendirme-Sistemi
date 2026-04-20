package com.bitirme.config;

import com.bitirme.entity.ModelVersion;
import com.bitirme.repository.ModelVersionRepository;
import com.bitirme.repository.NewsClassificationResultRepository;
import com.bitirme.repository.NewsRepository;
import com.bitirme.service.NewsClassificationService;
import com.bitirme.service.NewsCrawlerService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasks {

    private final NewsCrawlerService newsCrawlerService;
    private final NewsClassificationService newsClassificationService;
    private final ModelVersionRepository modelVersionRepository;
    private final NewsRepository newsRepository;
    private final NewsClassificationResultRepository newsClassificationResultRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Value("${scheduler.delete-refresh.enabled:false}")
    private boolean deleteRefreshEnabled;

    // Her 6 saatte bir haber çek (cron: 0 0 */6 * * ?)
    @Scheduled(cron = "0 0 */6 * * ?")
    public void crawlNewsScheduled() {
        log.info("Scheduled news crawling started...");
        try {
            int fetchedCount = newsCrawlerService.crawlAllSources();
            log.info("Scheduled news crawling completed. {} news fetched.", fetchedCount);
        } catch (Exception e) {
            log.error("Error in scheduled news crawling: {}", e.getMessage());
        }
    }

    // Her 30 dakikada bir son dakika haberlerini çek (cron: 0 */30 * * * ?)
    @Scheduled(cron = "0 */30 * * * ?")
    public void crawlBreakingNewsScheduled() {
        log.info("Scheduled breaking news crawling started...");
        try {
            int fetchedCount = newsCrawlerService.crawlBreakingNews();
            log.info("Scheduled breaking news crawling completed. {} news fetched.", fetchedCount);
        } catch (Exception e) {
            log.error("Error in scheduled breaking news crawling: {}", e.getMessage());
        }
    }

    // Her 1 saatte bir işlenmemiş haberleri kategorilendir (cron: 0 0 * * * ?)
    @Scheduled(cron = "0 0 * * * ?")
    public void classifyNewsScheduled() {
        log.info("Scheduled news classification started...");
        try {
            // Use the first available model version, or create a default one
            ModelVersion modelVersion = modelVersionRepository.findAll().stream()
                    .findFirst()
                    .orElseGet(() -> {
                        log.warn("No model version found, skipping classification");
                        return null;
                    });

            if (modelVersion != null) {
                int classifiedCount = newsClassificationService.classifyUnprocessedNews(modelVersion.getId());
                log.info("Scheduled news classification completed. {} news classified.", classifiedCount);
            }
        } catch (Exception e) {
            log.error("Error in scheduled news classification: {}", e.getMessage());
        }
    }

    // Her 5 saatte bir haber kayıtlarını sil ve yeni haberleri çek (00:00, 05:00, 10:00, 15:00, 20:00)
    @Scheduled(cron = "0 0 */5 * * ?")
    @Transactional
    public void deleteAndRefreshNews() {
        if (!deleteRefreshEnabled) {
            return;
        }
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("Scheduled news deletion and refresh started...");
        try {
            // Önce mevcut haberleri say ve bilgilerini kaydet
            long newsCount = newsRepository.count();
            long classificationCount = newsClassificationResultRepository.count();
            
            // Eski haberlerin detaylı bilgilerini log'la
            log.info("═══════════════════════════════════════════════════════════════");
            log.info("📊 SİLİNECEK HABER İSTATİSTİKLERİ:");
            log.info("   • Toplam haber sayısı: {}", newsCount);
            log.info("   • Toplam sınıflandırma sonucu: {}", classificationCount);
            
            if (newsCount > 0) {
                // Kaynak bazında dağılım
                var sourceStats = newsRepository.findAll().stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                news -> news.getSource() != null ? news.getSource().getName() : "Bilinmeyen",
                                java.util.stream.Collectors.counting()));
                
                log.info("   • Kaynak bazında dağılım:");
                sourceStats.forEach((source, count) -> 
                        log.info("     - {}: {} haber", source, count));
                
                // En eski ve en yeni haber tarihleri
                var dates = newsRepository.findAll().stream()
                        .map(news -> news.getPublishedAt())
                        .filter(java.util.Objects::nonNull)
                        .sorted()
                        .toList();
                
                if (!dates.isEmpty()) {
                    log.info("   • En eski haber: {}", dates.get(0));
                    log.info("   • En yeni haber: {}", dates.get(dates.size() - 1));
                }
            }
            log.info("═══════════════════════════════════════════════════════════════");
            
            // Önce sınıflandırma sonuçlarını sil (foreign key constraint için)
            newsClassificationResultRepository.deleteAll();
            log.info("✅ Deleted {} news classification results", classificationCount);

            // Sonra haberleri sil
            newsRepository.deleteAll();
            log.info("✅ Deleted {} news records", newsCount);
            
            // ID sequence'ini sıfırla (1'den başlaması için)
            entityManager.createNativeQuery("ALTER SEQUENCE news_id_seq RESTART WITH 1").executeUpdate();
            entityManager.createNativeQuery("ALTER SEQUENCE news_classification_results_id_seq RESTART WITH 1").executeUpdate();
            log.info("✅ Reset news ID sequence to start from 1");
            
            log.info("═══════════════════════════════════════════════════════════════");
            log.info("📥 Yeni haberler çekiliyor...");
            
            // Yeni haberleri çek
            int fetchedCount = newsCrawlerService.crawlAllSources();
            
            log.info("═══════════════════════════════════════════════════════════════");
            log.info("✅ Scheduled news refresh completed successfully!");
            log.info("   • Silinen haber sayısı: {}", newsCount);
            log.info("   • Silinen sınıflandırma: {}", classificationCount);
            log.info("   • Yeni çekilen haber: {}", fetchedCount);
            log.info("═══════════════════════════════════════════════════════════════");
        } catch (Exception e) {
            log.error("❌ Error in scheduled news deletion and refresh: {}", e.getMessage(), e);
        }
    }
}

