package com.bitirme.service;

import com.bitirme.dto.news.NewsClassificationResultCreateRequest;
import com.bitirme.entity.*;
import com.bitirme.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsClassificationService {

    private final NewsRepository newsRepository;
    private final CategoryRepository categoryRepository;
    private final ModelVersionRepository modelVersionRepository;
    private final NewsClassificationResultService classificationResultService;

    private static final Map<String, List<String>> CATEGORY_KEYWORDS = new HashMap<>();

    static {
        CATEGORY_KEYWORDS.put("Asayiş", Arrays.asList(
                "cinayet", "ölüm", "kaza", "tutuklama", "ceza", "mahkeme", "dava", "şüpheli", "suç",
                "operasyon", "gözaltı", "şebeke", "organize suç", "yolsuzluk", "hırsızlık", "dolandırıcılık",
                "öldür", "boğul", "feci olay", "korkunç olay", "katled", "bıçakla", "silah", "terör",
                "bomba", "patlama", "kaçakçılık", "uyuşturucu", "fetö", "pdy", "terör örgütü",
                "adliye", "savcı", "hakim", "mahkum", "hapis", "tutuklu", "sanık", "duruşma", "adli kontrol"
        ));
        CATEGORY_KEYWORDS.put("Siyaset", Arrays.asList(
                "bakan", "meclis", "parti", "seçim", "hükümet", "cumhurbaşkanı", "dışişleri", "terör",
                "başkan", "lider", "miting", "chp", "akp", "mhp", "iyi parti", "demokratik parti",
                "siyasi", "diplomatik", "uluslararası", "dış politika", "kamu", "devlet", "pakistan",
                "başbakan", "dışişleri bakanı", "büyükelçi", "diplomat"
        ));
        CATEGORY_KEYWORDS.put("Spor", Arrays.asList(
                "futbol", "basketbol", "spor", "milli takım", "şampiyonlar ligi", "süper lig",
                "galatasaray", "fenerbahçe", "beşiktaş", "trabzonspor", "maç", "turnuva", "şampiyona",
                "sporcu", "antrenman", "etnospor", "spor kulübü", "spor federasyonu", "olimpiyat"
        ));
        CATEGORY_KEYWORDS.put("Turizm", Arrays.asList(
                "turizm", "seyahat", "tatil", "otel", "rezervasyon", "tur", "gezi", "turist",
                "seyahat acentesi", "uçak", "bilet", "nemrut", "antik kent", "müze", "tarihi eser",
                "kültür turu", "doğa turu", "ziyaretçi", "turizm sezonu", "turizm bakanlığı"
        ));
        CATEGORY_KEYWORDS.put("Teknoloji", Arrays.asList(
                "teknoloji", "yazılım", "bilgisayar", "telefon", "internet", "dijital", "yapay zeka",
                "siber", "uygulama", "donanım", "inovasyon", "startup", "sosyal medya", "dijital varlık",
                "blockchain", "kripto", "yazılım şirketi", "teknoloji şirketi", "akıllı telefon"
        ));
        CATEGORY_KEYWORDS.put("Sağlık", Arrays.asList(
                "sağlık", "hastane", "doktor", "tedavi", "ilaç", "korona", "virüs", "hastalık", "aşı",
                "klinik", "ameliyat", "check-up", "muayene", "sağlık bakanlığı", "tıp", "sağlık hizmeti",
                "hasta", "sağlık personeli", "sağlık çalışanı"
        ));
        CATEGORY_KEYWORDS.put("Doğal Afet", Arrays.asList(
                "deprem", "sel", "fırtına", "taşkın", "afet", "heyelan", "çığ", "kuraklık", "yangın",
                "doğal afet", "meydana geldi", "hasar", "zede", "kayıp", "felaket", "afet bölgesi",
                "itfaiye", "afad", "kurtarma ekibi"
        ));
        CATEGORY_KEYWORDS.put("Ulaşım", Arrays.asList(
                "trafik", "metro", "otobüs", "tren", "havayolu", "karayolu", "denizyolu", "ulaşım",
                "seyahat", "yol", "kaza", "araba", "şoför", "sürücü", "yolcu", "toplu taşıma",
                "trafik yoğunluğu", "trafik kazası", "trafik polisi"
        ));
        CATEGORY_KEYWORDS.put("Çevre", Arrays.asList(
                "çevre", "doğa", "iklim", "hava", "su", "toprak", "kirlilik", "geri dönüşüm", "atık",
                "enerji", "yeşil", "sürdürülebilir", "ekoloji", "biyoçeşitlilik", "çevre kirliliği",
                "doğal kaynak", "petrol", "rezerv", "doğal gaz", "enerji kaynağı"
        ));
        CATEGORY_KEYWORDS.put("Ekonomi", Arrays.asList(
                "ekonomi", "piyasa", "borsa", "dolar", "euro", "altın", "faiz", "enflasyon", "yatırım",
                "şirket", "ticaret", "ihracat", "ithalat", "sanayi", "üretim", "tüketim", "ekonomik kriz",
                "ekonomi bakanlığı", "merkez bankası", "ekonomi politikası"
        ));
        CATEGORY_KEYWORDS.put("Magazin", Arrays.asList(
                "magazin", "sanatçı", "oyuncu", "show", "dizi", "film", "müzik", "konser", "festival",
                "ödül", "röportaj", "sanat", "kültür", "tiyatro", "sergi", "sinema", "sanat etkinliği",
                "kültür sanat", "festival", "ünlü", "star", "düğün", "nişan", "evlilik", "boşanma"
        ));
        
        CATEGORY_KEYWORDS.put("Kültür-Sanat", Arrays.asList(
                "kültür", "sanat", "müze", "sergi", "tiyatro", "opera", "bale", "resim", "heykel",
                "edebiyat", "kitap", "yazar", "şair", "roman", "şiir", "konser", "müzik", "enstrüman",
                "galeri", "antik", "tarihi", "arkeoloji"
        ));
        
        CATEGORY_KEYWORDS.put("Eğitim", Arrays.asList(
                "eğitim", "okul", "üniversite", "öğrenci", "öğretmen", "sınav", "yks", "ales", "dgs",
                "bakanlık", "meb", "yök", "üniversite", "fakülte", "bölüm", "mezun", "diploma",
                "burs", "staj", "eğitim sistemi", "müfredat"
        ));
        
        CATEGORY_KEYWORDS.put("Bilim-Teknoloji", Arrays.asList(
                "bilim", "araştırma", "teknoloji", "yapay zeka", "robot", "uzay", "nasa", "mars",
                "yenilik", "inovasyon", "startup", "yazılım", "donanım", "bilgisayar", "telefon",
                "tablet", "internet", "dijital", "siber", "blockchain", "kripto"
        ));
        
        CATEGORY_KEYWORDS.put("Dünya", Arrays.asList(
                "dünya", "uluslararası", "abd", "amerika", "avrupa", "asya", "afrika", "çin", "rusya",
                "almanya", "fransa", "ingiltere", "brexit", "nato", "bm", "birleşmiş milletler",
                "diplomasi", "dış politika", "büyükelçi", "konsolosluk"
        ));
        
        CATEGORY_KEYWORDS.put("Sosyal", Arrays.asList(
                "sosyal", "toplum", "sosyal medya", "facebook", "twitter", "instagram", "youtube",
                "influencer", "blogger", "trend", "popüler", "gündem", "hashtag", "viral"
        ));
    }

    @Transactional
    public int classifyUnprocessedNews(Integer modelVersionId) {
        List<News> unprocessedNews = newsRepository.findByProcessedFalse();
        ModelVersion modelVersion = modelVersionRepository.findById(modelVersionId)
                .orElseThrow(() -> new com.bitirme.exception.NotFoundException("Model versiyonu bulunamadı: " + modelVersionId));

        int classifiedCount = 0;

        for (News news : unprocessedNews) {
            try {
                ClassificationResult result = classifyNews(news);
                if (result != null) {
                    saveClassificationResult(news, modelVersion, result);
                    news.setProcessed(true);
                    newsRepository.save(news);
                    classifiedCount++;
                }
            } catch (Exception e) {
                log.error("Error classifying news {}: {}", news.getId(), e.getMessage());
            }
        }

        return classifiedCount;
    }

    @Transactional
    public void classifyNews(Long newsId, Integer modelVersionId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new com.bitirme.exception.NotFoundException("Haber bulunamadı: " + newsId));
        ModelVersion modelVersion = modelVersionRepository.findById(modelVersionId)
                .orElseThrow(() -> new com.bitirme.exception.NotFoundException("Model versiyonu bulunamadı: " + modelVersionId));

        ClassificationResult result = classifyNews(news);
        if (result != null) {
            saveClassificationResult(news, modelVersion, result);
            news.setProcessed(true);
            newsRepository.save(news);
        }
    }

    private ClassificationResult classifyNews(News news) {
        String text = (news.getTitle() + " " + (news.getContent() != null ? news.getContent() : ""))
                .toLowerCase();

        Map<String, Integer> categoryScores = new HashMap<>();

        // Calculate scores for each category
        for (Map.Entry<String, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            String category = entry.getKey();
            List<String> keywords = entry.getValue();
            int score = 0;

            for (String keyword : keywords) {
                if (text.contains(keyword)) {
                    score++;
                }
            }

            if (score > 0) {
                categoryScores.put(category, score);
            }
        }

        // Find the category with the highest score
        if (categoryScores.isEmpty()) {
            // Try to match with existing categories in database
            return matchWithDatabaseCategories(text);
        }

        String predictedCategoryName = categoryScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Diğer");

        int maxScore = categoryScores.get(predictedCategoryName);
        int totalKeywords = CATEGORY_KEYWORDS.get(predictedCategoryName).size();
        BigDecimal confidence = BigDecimal.valueOf(maxScore)
                .divide(BigDecimal.valueOf(totalKeywords), 4, RoundingMode.HALF_UP)
                .min(BigDecimal.ONE);

        return new ClassificationResult(predictedCategoryName, confidence);
    }

    private ClassificationResult matchWithDatabaseCategories(String text) {
        List<Category> categories = categoryRepository.findAll();
        String textLower = text.toLowerCase();

        // Try to match with category names
        for (Category category : categories) {
            String categoryName = category.getName().toLowerCase();
            if (textLower.contains(categoryName)) {
                return new ClassificationResult(category.getName(), BigDecimal.valueOf(0.5));
            }
        }

        // Default to "Diğer" or first available category
        Optional<Category> otherCategory = categoryRepository.findByName("Diğer");
        if (otherCategory.isPresent()) {
            return new ClassificationResult("Diğer", BigDecimal.valueOf(0.1));
        } else if (!categories.isEmpty()) {
            return new ClassificationResult(categories.get(0).getName(), BigDecimal.valueOf(0.1));
        }

        return null;
    }

    private void saveClassificationResult(News news, ModelVersion modelVersion, ClassificationResult result) {
        Category category = categoryRepository.findByName(result.categoryName)
                .orElseGet(() -> {
                    // If category doesn't exist, create it or use first available
                    Category newCategory = new Category();
                    newCategory.setName(result.categoryName);
                    newCategory.setDescription("Otomatik oluşturulan kategori");
                    newCategory.setActive(true);
                    return categoryRepository.save(newCategory);
                });

        NewsClassificationResultCreateRequest request = new NewsClassificationResultCreateRequest();
        request.setNewsId(news.getId());
        request.setModelVersionId(modelVersion.getId());
        request.setPredictedCategoryId(category.getId());
        request.setPredictionScore(result.confidence);
        request.setActive(true);

        classificationResultService.create(request);
    }

    private static class ClassificationResult {
        String categoryName;
        BigDecimal confidence;

        ClassificationResult(String categoryName, BigDecimal confidence) {
            this.categoryName = categoryName;
            this.confidence = confidence;
        }
    }
}

