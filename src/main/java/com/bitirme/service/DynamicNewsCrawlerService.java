package com.bitirme.service;

import com.bitirme.dto.news.NewsCreateRequest;
import com.bitirme.entity.CrawlingLog;
import com.bitirme.entity.News;
import com.bitirme.entity.Source;
import com.bitirme.repository.CrawlingLogRepository;
import com.bitirme.repository.NewsRepository;
import com.bitirme.repository.SourceRepository;
import com.bitirme.util.NewsTitleNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicNewsCrawlerService {

    private final NewsRepository newsRepository;
    private final SourceRepository sourceRepository;
    private final NewsService newsService;
    private final CrawlingLogRepository crawlingLogRepository;

    private static final List<String> USER_AGENTS = Arrays.asList(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:89.0) Gecko/20100101 Firefox/89.0"
    );

    private static final Set<String> GENERIC_TITLES = Set.of(
            "gündem", "gundem", "galeri", "video", "son dakika", "sondakika",
            "en çok okunanlar", "son dakika haberleri", "türkiye", "turkiye",
            "tüm haberler", "tum haberler", "haberler", "kurumsal", "kurumsal bilgiler",
            "iletişim", "iletisim", "abonelik", "rss",
            "politika", "3.sayfa", "3 sayfa",
            "güncel", "guncel", "ekonomi", "dünya", "dunya", "sağlık", "saglik",
            "eğitim", "egitim", "teknoloji", "spor", "magazin", "kültür-sanat", "kultur-sanat"
    );
    private static final Pattern ONLY_PUNCT_OR_DIGITS = Pattern.compile("^[\\p{Punct}\\d\\s]+$");

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

    public int crawlSource(Source source) {
        String url = source.getCrawlUrl() != null && !source.getCrawlUrl().isEmpty() 
                ? source.getCrawlUrl() 
                : (source.getBaseUrl() + source.getCategoryPath());
        return crawlSourceFromUrl(source, url, false);
    }

    public int crawlSourceFromUrl(Source source, String url, boolean isBreakingNews) {
        List<NewsCreateRequest> newsList = new ArrayList<>();

        // Her tarama için log kaydı oluştur
        CrawlingLog crawlingLog = new CrawlingLog();
        crawlingLog.setSource(source);
        crawlingLog.setStatus("RUNNING");
        crawlingLog.setFetchedCount(0);
        crawlingLogRepository.save(crawlingLog);

        int savedCount = 0;

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
                            throw new com.bitirme.exception.BusinessException("Retry sırasında kesinti oluştu", ie);
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
                            throw new com.bitirme.exception.BusinessException("Retry sırasında kesinti oluştu", ie);
                        }
                        retryDelay *= 2; // Exponential backoff
                        continue;
                    }
                    throw e; // Son denemede başarısız olursa fırlat
                }
            }
            
            if (doc == null) {
                throw new com.bitirme.exception.BusinessException("Belge " + maxRetries + " deneme sonrasında alınamadı");
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

                    // URL filtresi - sadece gerçekten geçersiz veya menü linklerini filtrele
                    String linkLower = link.toLowerCase();
                    
                    // Mutlaka geçilmesi gereken linkler
                    if (linkLower.contains("javascript:") ||
                        linkLower.startsWith("#") ||
                        linkLower.contains("mailto:") ||
                        linkLower.contains("tel:")) {
                        continue;
                    }
                    
                    // Ana sayfa linklerini ve kurumsal / menü sayfalarını geç
                    if (linkLower.equals(source.getBaseUrl().toLowerCase()) ||
                        linkLower.equals(source.getBaseUrl().toLowerCase() + "/") ||
                        linkLower.contains("/kurumsal") ||
                        linkLower.contains("/haberler") ||
                        linkLower.contains("/iletisim") ||
                        linkLower.contains("/rss")) {
                        continue;
                    }
                    
                    // Çok kısa linkleri geç (muhtemelen navigation linkleri)
                    String relativePath = linkLower.replace(source.getBaseUrl().toLowerCase(), "");
                    if (relativePath.length() > 0 && relativePath.length() < 5) {
                        continue;
                    }

                    seenLinks.add(link);

                    ArticleData articleData = fetchArticleData(link, element, source);
                    String title = articleData.title();
                    String content = articleData.content();

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
                        // Düşük kaliteli kayıtları (TRT Haber'de görülen "Gündem/GALERİ" vb.) hiç ekleme
                        String safeContent = (content != null && !content.isEmpty()) ? content : title;
                        if (isLowQuality(title, safeContent)) {
                            continue;
                        }
                        NewsCreateRequest news = new NewsCreateRequest();
                        news.setTitle(title);
                        news.setContent(safeContent);
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
            
            // Veritabanına kaydet - duplicate kontrolü ile (aynı haber asla iki kez eklenmez)
            int duplicateCount = 0;
            int externalIdDuplicateCount = 0;
            int titleDuplicateCount = 0;
            for (NewsCreateRequest newsRequest : newsList) {
                try {
                    // 1) Aynı kaynaktan aynı URL (external_id) zaten varsa ekleme
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

                    // 2) Aynı kaynaktan aynı başlık (normalize) zaten varsa ekleme - Takvim vb. aynı haber farklı URL tekrarını engeller
                    String normalizedTitle = NewsTitleNormalizer.normalize(newsRequest.getTitle());
                    if (!normalizedTitle.isEmpty() && normalizedTitle.length() >= 5) {
                        if (newsRepository.existsBySourceIdAndNormalizedTitle(source.getId(), normalizedTitle)) {
                            duplicateCount++;
                            titleDuplicateCount++;
                            log.debug("Source {}: Duplicate news skipped (same title already in DB for this source): {}", 
                                    source.getName(), newsRequest.getTitle());
                            continue;
                        }
                    }

                    // 3) Global duplicate kontrolünü kaldırdık; farklı kaynaklardan aynı başlık gelebilir

                    newsRequest.setSourceId(source.getId());
                    newsService.create(newsRequest);
                    savedCount++;
                } catch (Exception e) {
                    log.error("Error saving news: {}", e.getMessage());
                }
            }
            
            if (duplicateCount > 0) {
                log.info("Source {}: {} duplicate news skipped - External ID: {}, Same title: {}", 
                        source.getName(), duplicateCount, externalIdDuplicateCount, titleDuplicateCount);
            }
            
            log.info("Source {}: Total {} news fetched and saved to database ({} duplicates skipped)", 
                    source.getName(), savedCount, duplicateCount);

            // Log kaydını güncelle
            crawlingLog.setStatus("SUCCESS");
            crawlingLog.setFetchedCount(savedCount);
            crawlingLog.setFinishedAt(LocalDateTime.now());
            crawlingLogRepository.save(crawlingLog);

            return savedCount;
        } catch (org.jsoup.HttpStatusException httpEx) {
            log.warn("HTTP error crawling source {} from URL {}: Status={}, URL=[{}]", 
                    source.getName(), url, httpEx.getStatusCode(), url);

            crawlingLog.setStatus("FAILED");
            crawlingLog.setErrorMessage("HTTP " + httpEx.getStatusCode() + " - " + httpEx.getMessage());
            crawlingLog.setFinishedAt(LocalDateTime.now());
            crawlingLogRepository.save(crawlingLog);

            return 0;
        } catch (Exception e) {
            log.error("Error crawling source {} from URL {}: {}", source.getName(), url, e.getMessage());

            crawlingLog.setStatus("FAILED");
            crawlingLog.setErrorMessage(e.getMessage());
            crawlingLog.setFinishedAt(LocalDateTime.now());
            crawlingLogRepository.save(crawlingLog);

            return 0;
        }
    }

    private boolean isLowQuality(String title, String content) {
        if (title == null || title.isBlank()) return true;
        String t = normalizeSimple(title);
        String c = normalizeSimple(content != null ? content : "");

        if (t.length() < 8) return true;
        if (ONLY_PUNCT_OR_DIGITS.matcher(t).matches()) return true;
        if (GENERIC_TITLES.contains(t)) return true;

        // Title ve content aynıysa (ör. "Gündem" / "Gündem") kayıt etmeyelim
        if (!c.isEmpty() && t.equals(c)) return true;

        // Content çok kısa ise (title'dan farklı olsa bile) kayıt etmeyelim
        if (c.length() < 20) return true;

        return false;
    }

    private String normalizeSimple(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.forLanguageTag("tr"))
                .replaceAll("[^a-zçğıöşü0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
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

    private ArticleData fetchArticleData(String link, Element listElement, Source source) {
        String fallbackTitle = extractTitle(listElement, source);
        String fallbackContent = extractContent(listElement, source);

        try {
            Document articleDoc = Jsoup.connect(link)
                    .userAgent(getRandomUserAgent())
                    .timeout(20000)
                    .followRedirects(true)
                    .maxBodySize(0)
                    .get();

            String articleTitle = extractTitleFromArticlePage(articleDoc, source);
            String articleContent = extractContentFromArticlePage(articleDoc, source);

            if (articleTitle == null || articleTitle.isBlank()) {
                articleTitle = fallbackTitle;
            }
            if (articleContent == null || articleContent.length() < 40) {
                articleContent = fallbackContent;
            }

            return new ArticleData(articleTitle, articleContent);
        } catch (IOException e) {
            log.debug("Could not fetch article page for {}: {}", link, e.getMessage());
            return new ArticleData(fallbackTitle, fallbackContent);
        }
    }

    private String extractTitleFromArticlePage(Document doc, Source source) {
        List<String> selectors = new ArrayList<>();
        if (source.getTitleSelector() != null && !source.getTitleSelector().isBlank()) {
            selectors.add(source.getTitleSelector());
        }
        selectors.addAll(List.of(
                "meta[property=og:title]",
                "meta[name=twitter:title]",
                "article h1",
                "main h1",
                "h1",
                ".article-title",
                ".news-title",
                ".headline"
        ));

        for (String selector : selectors) {
            try {
                Element el = doc.selectFirst(selector);
                if (el == null) continue;
                String value = selector.startsWith("meta") ? el.attr("content").trim() : el.text().trim();
                if (value.length() > 5 && value.length() < 300) {
                    return value;
                }
            } catch (Exception ignored) {
            }
        }
        return doc.title();
    }

    private String extractContentFromArticlePage(Document doc, Source source) {
        List<String> selectors = new ArrayList<>();
        if (source.getContentSelector() != null && !source.getContentSelector().isBlank()) {
            selectors.add(source.getContentSelector());
        }
        selectors.addAll(List.of(
                "article p",
                "main article p",
                ".article-body p",
                ".news-detail p",
                ".detail-content p",
                ".content-detail p",
                ".haber-metni p",
                ".post-content p",
                ".entry-content p",
                "main p"
        ));

        for (String selector : selectors) {
            String joined = joinParagraphs(doc.select(selector));
            if (joined.length() >= 80) {
                return joined;
            }
        }

        Element metaDescription = doc.selectFirst("meta[name=description], meta[property=og:description]");
        if (metaDescription != null) {
            String description = metaDescription.attr("content").trim();
            if (description.length() >= 40) {
                return description;
            }
        }

        return "";
    }

    private String joinParagraphs(Elements paragraphs) {
        if (paragraphs == null || paragraphs.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Element p : paragraphs) {
            String text = p.text().trim();
            if (text.length() < 20) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(text);
            if (builder.length() >= 4000) {
                break;
            }
        }
        return builder.toString().replaceAll("\\s+", " ").trim();
    }

    private String getRandomUserAgent() {
        Random random = new Random();
        return USER_AGENTS.get(random.nextInt(USER_AGENTS.size()));
    }

    private record ArticleData(String title, String content) {
    }

}

