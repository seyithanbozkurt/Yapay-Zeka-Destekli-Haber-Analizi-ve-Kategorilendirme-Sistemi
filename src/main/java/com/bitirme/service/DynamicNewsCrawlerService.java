package com.bitirme.service;

import com.bitirme.dto.news.NewsCreateRequest;
import com.bitirme.entity.News;
import com.bitirme.entity.Source;
import com.bitirme.repository.NewsRepository;
import com.bitirme.repository.SourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicNewsCrawlerService {

    private final NewsRepository newsRepository;
    private final SourceRepository sourceRepository;
    private final NewsService newsService;

    private static final List<String> USER_AGENTS = Arrays.asList(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:89.0) Gecko/20100101 Firefox/89.0"
    );

    @Transactional
    public int crawlAllSources() {
        List<Source> activeSources = sourceRepository.findByActiveTrue();
        int totalFetched = 0;

        for (Source source : activeSources) {
            try {
                int count = crawlSource(source);
                totalFetched += count;
                log.info("Source {}: {} news fetched", source.getName(), count);
                Thread.sleep(2000); // Rate limiting
            } catch (Exception e) {
                log.error("Error crawling source {}: {}", source.getName(), e.getMessage());
            }
        }

        return totalFetched;
    }

    @Transactional
    public int crawlBreakingNews() {
        List<Source> sourcesWithBreakingNews = sourceRepository.findByActiveTrue().stream()
                .filter(s -> s.getCrawlType() != null && s.getCrawlType().equals("breaking_news"))
                .filter(s -> s.getLastMinuteUrl() != null && !s.getLastMinuteUrl().isEmpty())
                .toList();

        int totalFetched = 0;

        for (Source source : sourcesWithBreakingNews) {
            try {
                int count = crawlSourceFromUrl(source, source.getLastMinuteUrl(), true);
                totalFetched += count;
                log.info("Source {} (Breaking News): {} news fetched", source.getName(), count);
                Thread.sleep(2000);
            } catch (Exception e) {
                log.error("Error crawling breaking news from {}: {}", source.getName(), e.getMessage());
            }
        }

        return totalFetched;
    }

    @Transactional
    public int crawlSource(Source source) {
        String url = source.getCrawlUrl() != null && !source.getCrawlUrl().isEmpty() 
                ? source.getCrawlUrl() 
                : (source.getBaseUrl() + source.getCategoryPath());
        return crawlSourceFromUrl(source, url, false);
    }

    @Transactional
    public int crawlSourceFromUrl(Source source, String url, boolean isBreakingNews) {
        List<NewsCreateRequest> newsList = new ArrayList<>();

        try {
            // Retry mekanizması ile bağlantı kurma
            Document doc = null;
            int maxRetries = 3;
            int retryDelay = 2000; // 2 saniye
            
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    doc = Jsoup.connect(url)
                            .userAgent(getRandomUserAgent())
                            .timeout(20000) // Timeout'u 20 saniyeye çıkardık
                            .followRedirects(true)
                            .maxBodySize(0) // Body size limit'ini kaldırdık
                            .get();
                    break; // Başarılı olursa döngüden çık
                } catch (org.jsoup.HttpStatusException httpEx) {
                    // 504, 502, 503 gibi geçici hatalarda retry yap
                    if ((httpEx.getStatusCode() == 504 || httpEx.getStatusCode() == 502 || httpEx.getStatusCode() == 503) && attempt < maxRetries) {
                        log.warn("Source {}: HTTP {} error on attempt {}/{}, retrying in {}ms...", 
                                source.getName(), httpEx.getStatusCode(), attempt, maxRetries, retryDelay);
                        try {
                            Thread.sleep(retryDelay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interrupted during retry delay", ie);
                        }
                        retryDelay *= 2; // Exponential backoff
                        continue;
                    }
                    throw httpEx; // Son denemede başarısız olursa veya retry yapılmayacak bir hata varsa fırlat
                } catch (java.io.IOException e) {
                    // Timeout veya network hatalarında retry yap (SocketTimeoutException IOException'ın alt sınıfı)
                    if (attempt < maxRetries) {
                        log.warn("Source {}: Connection error on attempt {}/{}, retrying in {}ms... Error: {}", 
                                source.getName(), attempt, maxRetries, retryDelay, e.getMessage());
                        try {
                            Thread.sleep(retryDelay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interrupted during retry delay", ie);
                        }
                        retryDelay *= 2; // Exponential backoff
                        continue;
                    }
                    throw e; // Son denemede başarısız olursa fırlat
                }
            }
            
            if (doc == null) {
                throw new RuntimeException("Failed to fetch document after " + maxRetries + " attempts");
            }

            // Dinamik selector'lar kullan
            String linkSelector = source.getLinkSelector() != null && !source.getLinkSelector().isEmpty()
                    ? source.getLinkSelector()
                    : "a[href]";

            Elements linkElements = doc.select(linkSelector);
            log.info("Source {}: Found {} link elements with selector: {}", source.getName(), linkElements.size(), linkSelector);
            
            // Eğer selector ile link bulunamazsa veya çok az bulunduysa, fallback selector dene
            if (linkElements.size() < 5 && source.getLinkSelector() != null && !source.getLinkSelector().isEmpty()) {
                log.warn("Source {}: Only {} links found with selector '{}', trying fallback selectors", source.getName(), linkElements.size(), linkSelector);
                
                // Önce selector'ı virgülle ayırıp tek tek dene (birden fazla selector varsa)
                if (linkSelector.contains(",")) {
                    String[] selectors = linkSelector.split(",");
                    for (String altSelector : selectors) {
                        Elements altElements = doc.select(altSelector.trim());
                        if (altElements.size() > linkElements.size()) {
                            linkElements = altElements;
                            log.info("Source {}: Better selector found: {} ({} links)", source.getName(), altSelector.trim(), altElements.size());
                        }
                    }
                }
                
                // Hala yeterli link yoksa, kategori path ile dene
                if (linkElements.size() < 5) {
                    String categoryPathForFallback = source.getCategoryPath() != null ? source.getCategoryPath().replace("/", "") : "";
                    if (!categoryPathForFallback.isEmpty()) {
                        Elements categoryLinks = doc.select("a[href*='" + categoryPathForFallback + "']");
                        if (categoryLinks.size() > linkElements.size()) {
                            linkElements = categoryLinks;
                            log.info("Source {}: Category path fallback found {} links", source.getName(), categoryLinks.size());
                        }
                    }
                }
                
                // Hala yeterli değilse, genel haber selector'larını dene
                if (linkElements.size() < 5) {
                    Elements generalLinks = doc.select("article a, .news-item a, .article a, .haber-item a, .haber a, .news-card a");
                    if (generalLinks.size() > linkElements.size()) {
                        linkElements = generalLinks;
                        log.info("Source {}: General news selector found {} links", source.getName(), generalLinks.size());
                    }
                }
            }
            
            Set<String> seenLinks = new HashSet<>();
            int maxArticles = 50; // Limit articles per source
            int processedCount = 0;

            for (Element element : linkElements) {
                if (newsList.size() >= maxArticles) break;
                processedCount++;
                
                try {
                    String link = extractLink(element, source);
                    if (link == null || link.isEmpty() || seenLinks.contains(link)) {
                        continue;
                    }

                    // Base URL kontrolü
                    if (!link.startsWith("http")) {
                        if (link.startsWith("//")) {
                            link = "https:" + link;
                        } else if (link.startsWith("/")) {
                            link = source.getBaseUrl() + link;
                        } else {
                            link = source.getBaseUrl() + "/" + link;
                        }
                    }

                    // URL filtresi - sadece gerçekten geçersiz linkleri filtrele
                    String linkLower = link.toLowerCase();
                    
                    // Mutlaka geçilmesi gereken linkler
                    if (linkLower.contains("javascript:") ||
                        linkLower.startsWith("#") ||
                        linkLower.contains("mailto:") ||
                        linkLower.contains("tel:")) {
                        continue;
                    }
                    
                    // Ana sayfa linklerini geç (sadece base URL ise)
                    if (linkLower.equals(source.getBaseUrl().toLowerCase()) ||
                        linkLower.equals(source.getBaseUrl().toLowerCase() + "/")) {
                        continue;
                    }
                    
                    // Çok kısa linkleri geç (muhtemelen navigation linkleri)
                    String relativePath = linkLower.replace(source.getBaseUrl().toLowerCase(), "");
                    if (relativePath.length() > 0 && relativePath.length() < 5) {
                        continue;
                    }

                    seenLinks.add(link);

                    String title = extractTitle(element, source);
                    String content = extractContent(element, source);

                    // Title extraction başarısız olursa, link'ten veya element text'inden title çıkar
                    if (title == null || title.isEmpty() || title.length() < 5) {
                        // Link'ten title çıkarmayı dene
                        String linkText = element.text().trim();
                        if (linkText.length() > 5 && linkText.length() < 300) {
                            title = linkText;
                        } else if (element.hasAttr("title")) {
                            title = element.attr("title").trim();
                        } else {
                            // Link URL'den son kısmı al
                            String[] parts = link.split("/");
                            if (parts.length > 0) {
                                String lastPart = parts[parts.length - 1].replace("-", " ").replace("_", " ");
                                if (lastPart.length() > 5 && lastPart.length() < 100) {
                                    title = lastPart;
                                }
                            }
                        }
                    }

                    if (title != null && title.length() > 5 && title.length() < 300) {
                        NewsCreateRequest news = new NewsCreateRequest();
                        news.setTitle(title);
                        news.setContent(content != null && !content.isEmpty() ? content : title);
                        news.setOriginalUrl(link);
                        news.setExternalId(link);
                        news.setLanguage("tr");
                        news.setPublishedAt(LocalDateTime.now());
                        newsList.add(news);
                    } else {
                        log.debug("Source {}: Skipped link (title null or invalid): {}", source.getName(), link);
                    }
                } catch (Exception e) {
                    log.debug("Error parsing article from {}: {}", source.getName(), e.getMessage());
                }
            }

            log.info("Source {}: Processed {} links, extracted {} valid news items (title length > 5)", 
                    source.getName(), processedCount, newsList.size());
            
            // Veritabanına kaydet - duplicate kontrolü ile
            int savedCount = 0;
            int duplicateCount = 0;
            int externalIdDuplicateCount = 0;
            int titleDuplicateCount = 0;
            for (NewsCreateRequest newsRequest : newsList) {
                try {
                    // Önce external_id kontrolü (aynı kaynaktan aynı URL)
                    if (newsRequest.getExternalId() != null) {
                        Optional<News> existing = newsRepository
                                .findByExternalIdAndSourceId(newsRequest.getExternalId(), source.getId());
                        if (existing.isPresent()) {
                            duplicateCount++;
                            externalIdDuplicateCount++;
                            log.debug("Source {}: Duplicate news found (same external_id): {}", 
                                    source.getName(), newsRequest.getExternalId());
                            continue;
                        }
                    }
                    
                    // Title bazlı duplicate kontrolü (farklı kaynaklardan aynı haber)
                    String normalizedTitle = normalizeTitle(newsRequest.getTitle());
                    if (!normalizedTitle.isEmpty() && normalizedTitle.length() > 10) { // En az 10 karakter olmalı
                        List<News> similarNews = newsRepository.findByNormalizedTitle(normalizedTitle);
                        if (!similarNews.isEmpty()) {
                            duplicateCount++;
                            titleDuplicateCount++;
                            log.debug("Source {}: Duplicate news found (similar title already exists): {} (normalized: {})", 
                                    source.getName(), newsRequest.getTitle(), normalizedTitle);
                            continue;
                        }
                    }

                    newsRequest.setSourceId(source.getId());
                    newsService.create(newsRequest);
                    savedCount++;
                } catch (Exception e) {
                    log.error("Error saving news: {}", e.getMessage());
                }
            }
            
            if (duplicateCount > 0) {
                log.info("Source {}: {} duplicate news skipped - External ID duplicates: {}, Title duplicates: {}", 
                        source.getName(), duplicateCount, externalIdDuplicateCount, titleDuplicateCount);
            }
            
            log.info("Source {}: Total {} news fetched and saved to database ({} duplicates skipped)", 
                    source.getName(), savedCount, duplicateCount);

            return savedCount;
        } catch (org.jsoup.HttpStatusException httpEx) {
            log.warn("HTTP error crawling source {} from URL {}: Status={}, URL=[{}]", 
                    source.getName(), url, httpEx.getStatusCode(), url);
            return 0;
        } catch (Exception e) {
            log.error("Error crawling source {} from URL {}: {}", source.getName(), url, e.getMessage());
            return 0;
        }
    }

    private String extractLink(Element element, Source source) {
        if (element.hasAttr("href")) {
            return element.attr("href");
        }
        Element linkParent = element.parent();
        int depth = 0;
        while (linkParent != null && depth < 5) {
            if (linkParent.hasAttr("href")) {
                return linkParent.attr("href");
            }
            linkParent = linkParent.parent();
            depth++;
        }
        return null;
    }

    private String extractTitle(Element element, Source source) {
        // Önce custom selector'ı dene - parent container'da ara
        if (source.getTitleSelector() != null && !source.getTitleSelector().isEmpty()) {
            try {
                // Önce element'in kendisinde dene
                Element titleElement = element.selectFirst(source.getTitleSelector());
                if (titleElement == null) {
                    // Parent container'larda ara
                    Element parent = element.parent();
                    int depth = 0;
                    while (parent != null && depth < 5 && titleElement == null) {
                        titleElement = parent.selectFirst(source.getTitleSelector());
                        if (titleElement != null) break;
                        parent = parent.parent();
                        depth++;
                    }
                }
                if (titleElement != null) {
                    String title = titleElement.text().trim();
                    if (!title.isEmpty() && title.length() > 5) {
                        return title;
                    }
                }
            } catch (Exception e) {
                log.debug("Error using custom title selector: {}", e.getMessage());
            }
        }

        // Default title extraction strategies
        if (element.hasAttr("title")) {
            String title = element.attr("title").trim();
            if (!title.isEmpty() && title.length() > 5) {
                return title;
            }
        }

        // Try heading elements in parent containers
        Element parent = element.parent();
        int depth = 0;
        while (parent != null && depth < 5) {
            Element heading = parent.selectFirst("h1, h2, h3, h4, h5");
            if (heading != null) {
                String title = heading.text().trim();
                if (!title.isEmpty() && title.length() > 5 && title.length() < 300) {
                    return title;
                }
            }
            parent = parent.parent();
            depth++;
        }

        // Fallback to element text (if it's a heading element itself)
        if (element.tagName().matches("h[1-6]")) {
            String text = element.text().trim();
            if (text.length() > 5 && text.length() < 300) {
                return text;
            }
        }

        return null;
    }

    private String extractContent(Element element, Source source) {
        if (source.getContentSelector() != null && !source.getContentSelector().isEmpty()) {
            // Önce element'in kendisinde dene
            Element contentElement = element.selectFirst(source.getContentSelector());
            if (contentElement == null) {
                // Parent container'larda ara
                Element parent = element.parent();
                int depth = 0;
                while (parent != null && depth < 5 && contentElement == null) {
                    contentElement = parent.selectFirst(source.getContentSelector());
                    if (contentElement != null) break;
                    parent = parent.parent();
                    depth++;
                }
            }
            if (contentElement != null) {
                String content = contentElement.text().trim();
                if (!content.isEmpty()) {
                    return content;
                }
            }
        }

        // Default content extraction - parent container'dan al
        Element parent = element.parent();
        int depth = 0;
        while (parent != null && depth < 3) {
            Element p = parent.selectFirst("p");
            if (p != null) {
                String content = p.text().trim();
                if (!content.isEmpty() && content.length() > 20) {
                    return content;
                }
            }
            parent = parent.parent();
            depth++;
        }

        // Fallback
        return element.text().trim();
    }

    private String getRandomUserAgent() {
        Random random = new Random();
        return USER_AGENTS.get(random.nextInt(USER_AGENTS.size()));
    }
    
    /**
     * Title'ı normalize eder - duplicate kontrolü için kullanılır.
     * Küçük harfe çevirir, özel karakterleri ve fazla boşlukları temizler.
     * Türkçe karakterler korunur (ı, ğ, ü, ş, ö, ç).
     */
    private String normalizeTitle(String title) {
        if (title == null || title.isEmpty()) {
            return "";
        }
        
        // Küçük harfe çevir (Türkçe karakterler dahil)
        String normalized = title.toLowerCase();
        
        // Özel karakterleri kaldır (sadece harf, rakam, Türkçe karakterler ve boşluk bırak)
        // Türkçe karakterler: çğıöşüÇĞIİÖŞÜ
        normalized = normalized.replaceAll("[^a-zçğıöşü0-9\\s]", "");
        
        // Fazla boşlukları tek boşluğa indirge
        normalized = normalized.replaceAll("\\s+", " ").trim();
        
        return normalized;
    }
}

