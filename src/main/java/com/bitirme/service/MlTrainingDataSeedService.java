package com.bitirme.service;

import com.bitirme.entity.Category;
import com.bitirme.entity.ModelVersion;
import com.bitirme.entity.News;
import com.bitirme.entity.NewsClassificationResult;
import com.bitirme.entity.Source;
import com.bitirme.nlp.SparkNewsClassifier;
import com.bitirme.nlp.config.MlClassifierProperties;
import com.bitirme.repository.CategoryRepository;
import com.bitirme.repository.ModelVersionRepository;
import com.bitirme.repository.NewsClassificationResultRepository;
import com.bitirme.repository.NewsRepository;
import com.bitirme.repository.SourceRepository;
import com.bitirme.util.NewsTitleNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Yapay zeka destekli sınıflandırıcı için: her aktif kategoriden etiketli haber üretir,
 * toplam tekil etiketli haber sayısı {@code ml.classifier.min-training-samples} altına düşmeyecek şekilde dengeler,
 * ardından Spark (ve Naive Bayes) eğitimini senkron çalıştırır ve Spark modelini diske yükler.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MlTrainingDataSeedService {

    private static final String SEED_SOURCE_NAME = "YZ-AI-Egitim";

    /** Her kategoride mutlak minimum etiketli haber (ayrica min-training-samples / K ile ust sinir hesaplanir). */
    @Value("${app.ml.seed.min-per-category:4}")
    private int seedMinPerCategory;

    private final CategoryRepository categoryRepository;
    private final NewsRepository newsRepository;
    private final NewsClassificationResultRepository classificationResultRepository;
    private final ModelVersionRepository modelVersionRepository;
    private final SourceRepository sourceRepository;
    private final MlClassifierProperties mlClassifierProperties;
    private final MlModelTrainingService mlModelTrainingService;
    private final Optional<SparkNewsClassifier> sparkNewsClassifier;
    private final PlatformTransactionManager transactionManager;

    /**
     * 1) Her aktif kategori: en az {@code max(app.ml.seed.min-per-category, ceil(minSamples / K))} adet aktif etiketli haber.
     * 2) Tüm veritabanında tekil etiketli haber sayısı {@code minSamples} altındaysa, en az kayda sahip kategorilere
     *    sırayla ek haber (su doldurma) eklenir — böylece her kategoride içerik kalır ve toplam eşik sağlanır.
     */
    public Map<String, Object> seedBalancedLabelsAndTrainSync() {
        Map<String, Object> out = new HashMap<>();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Map<String, Object> seed = tx.execute(status -> executeSeedInserts());
        if (seed == null) {
            out.put("error", "Seed transaction sonucu null.");
            return out;
        }
        if (seed.containsKey("error")) {
            return seed;
        }
        out.putAll(seed);

        MlModelTrainingService.TrainingOutcome training = mlModelTrainingService.runFullTrainingSync();
        out.put("trainingMessage", training.message());
        out.put("trainingData", training.data());

        sparkNewsClassifier.ifPresent(sc -> {
            try {
                sc.loadModel();
                out.put("sparkModelLoaded", sc.isModelLoaded());
            } catch (Throwable t) {
                log.warn("Spark loadModel after train: {}", t.getMessage());
                out.put("sparkModelLoaded", false);
                out.put("sparkLoadError", t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        });
        if (sparkNewsClassifier.isEmpty()) {
            out.put("sparkModelLoaded", false);
            out.put("sparkNote", "ml.classifier.enabled=false — Spark bean yok.");
        }

        return out;
    }

    /** Kısa transaction: yalnızca haber + etiket ekleri */
    private Map<String, Object> executeSeedInserts() {
        Map<String, Object> out = new HashMap<>();
        int minSamples = Math.max(20, mlClassifierProperties.getMinTrainingSamples());

        List<Category> categories = categoryRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getActive()))
                .filter(c -> c.getName() != null && !c.getName().isBlank())
                .sorted(Comparator.comparing(Category::getName))
                .toList();

        if (categories.isEmpty()) {
            out.put("error", "Veritabanında aktif kategori yok.");
            return out;
        }

        ModelVersion modelVersion = modelVersionRepository.findAll().stream()
                .findFirst()
                .orElse(null);
        if (modelVersion == null) {
            out.put("error", "model_versions tablosunda kayıt yok; DataInitializer çalışmış olmalı.");
            return out;
        }

        Source source = resolveSeedSource();
        int k = categories.size();
        int fromTotal = (int) Math.ceil((double) minSamples / Math.max(1, k));
        int floorPerCategory = Math.max(1, seedMinPerCategory);
        int perCategory = Math.max(floorPerCategory, fromTotal);

        Map<String, Integer> createdByCategory = new HashMap<>();
        int createdTotal = 0;
        int contentVariant = 0;

        for (Category category : categories) {
            long existing = classificationResultRepository.countByPredictedCategoryIdAndActiveTrue(category.getId());
            int need = (int) Math.max(0, perCategory - existing);
            for (int i = 0; i < need; i++) {
                createOneLabeledNews(source, modelVersion, category, contentVariant++);
                createdTotal++;
                createdByCategory.merge(category.getName(), 1, Integer::sum);
            }
        }

        int balanceAdded = 0;
        int safety = 0;
        while (classificationResultRepository.countDistinctNewsWithActiveClassification() < minSamples
                && safety++ < 500) {
            Category poorest = categories.stream()
                    .min(Comparator.comparingLong(c ->
                            classificationResultRepository.countByPredictedCategoryIdAndActiveTrue(c.getId())))
                    .orElse(null);
            if (poorest == null) {
                break;
            }
            createOneLabeledNews(source, modelVersion, poorest, contentVariant++);
            balanceAdded++;
            createdTotal++;
            createdByCategory.merge(poorest.getName(), 1, Integer::sum);
        }

        out.put("minSamplesTarget", minSamples);
        out.put("categoriesConsidered", k);
        out.put("seedMinPerCategory", floorPerCategory);
        out.put("targetPerCategory", perCategory);
        out.put("syntheticNewsCreated", createdTotal);
        out.put("balanceRoundAdded", balanceAdded);
        out.put("distinctLabeledNewsAfterSeed",
                classificationResultRepository.countDistinctNewsWithActiveClassification());
        out.put("createdByCategoryName", createdByCategory);
        return out;
    }

    private void createOneLabeledNews(Source source, ModelVersion modelVersion, Category category, int contentVariant) {
        String slug = category.getId() + "-" + contentVariant + "-" + UUID.randomUUID().toString().substring(0, 8);
        String title = "YZ-AI örnek " + (contentVariant + 1) + ": " + category.getName() + " gündemi " + slug;
        String content = corpusForCategory(category.getName(), contentVariant);

        News news = new News();
        news.setSource(source);
        news.setExternalId("yz-ai-" + slug);
        news.setTitle(title);
        news.setNormalizedTitle(NewsTitleNormalizer.normalize(title));
        news.setContent(content);
        news.setOriginalUrl("https://local.invalid/yz-ai/" + slug);
        news.setPublishedAt(LocalDateTime.now());
        news.setProcessed(true);
        news.getCategories().add(category);
        newsRepository.save(news);

        NewsClassificationResult result = new NewsClassificationResult();
        result.setNews(news);
        result.setModelVersion(modelVersion);
        result.setPredictedCategory(category);
        result.setPredictionScore(new BigDecimal("0.9500"));
        result.setActive(true);
        classificationResultRepository.save(result);
    }

    private Source resolveSeedSource() {
        return sourceRepository.findByName(SEED_SOURCE_NAME).orElseGet(() -> {
            Source s = new Source();
            s.setName(SEED_SOURCE_NAME);
            s.setBaseUrl("https://local.invalid");
            s.setActive(true);
            s.setCrawlType("general");
            return sourceRepository.save(s);
        });
    }

    /** Kategoriye özgü metin + varyant ile farklı kelime dağarcığı (aynı kategoriden birden çok haber). */
    private static String corpusForCategory(String name, int contentVariant) {
        String label = name != null ? name : "Genel";
        String base;
        if (name == null) {
            base = unknownCategoryBaseText("Genel");
        } else {
            base = switch (name) {
            case "Spor" -> "Milli takım hazırlıkları sürüyor. Süper Lig'de haftanın maçları ve puan durumu güncellendi. "
                    + "Basketbol şampiyonlar ligi eleme turunda temsilcilerimiz sahada. Futbol federasyonu açıklama yaptı.";
            case "Ekonomi" -> "Merkez Bankası faiz kararı sonrası piyasalar hareketlendi. Döviz kurları ve borsa endeksi gün içinde değişti. "
                    + "Enflasyon verileri ve büyüme beklentileri ekonomi gündeminde öne çıktı.";
            case "Teknoloji" -> "Yapay zeka uygulamaları ve bulut bilişim yatırımları artıyor. Siber güvenlik uyarıları paylaşıldı. "
                    + "Yazılım şirketleri yeni ürün duyurularıyla teknoloji sektörüne yön veriyor.";
            case "Siyaset" -> "Meclis gündeminde yasa teklifleri görüşüldü. Cumhurbaşkanlığı ve bakanlıklar açıklamalar yayımladı. "
                    + "Yerel seçim hazırlıkları siyaset kulislerinde konuşuluyor.";
            case "Sağlık" -> "Sağlık Bakanlığı günlük vaka tablosunu paylaştı. Hastanelerde randevu düzeni ve aşı programı gündemde. "
                    + "Uzmanlar beslenme ve koruyucu sağlık konusunda uyarılarda bulundu.";
            case "Eğitim" -> "MEB akademik takvimi ve sınav tarihleri duyuruldu. Üniversiteler kontenjan ve burs başvurularını açtı. "
                    + "Okullarda dijital eğitim içerikleri genişletiliyor.";
            case "Asayiş" -> "Emniyet ekipleri operasyon düzenledi; gözaltı ve soruşturma süreçleri devam ediyor. "
                    + "Mahkeme kararları ve adli süreçler asayiş haberleri kapsamında yer aldı.";
            case "Turizm" -> "Otel rezervasyonları ve uçuş trafiği turizm sezonunda arttı. Antalya ve Kapadokya bölgesinde konaklama doluluk oranları yükseldi. "
                    + "Kültür ve turizm bakanlığı tanıtım programlarını duyurdu.";
            case "Doğal Afet" -> "Meteoroloji genel müdürlüğü uyarıları yayımladı. Deprem ve sel riski bulunan bölgelerde tedbirler anlatıldı. "
                    + "Afet ve acil durum yönetimi koordinasyon toplantısı yapıldı.";
            case "Ulaşım" -> "Toplu taşımada yeni hat ve sefer düzenlemeleri devreye alındı. Otoyol ve köprü geçiş ücretleri güncellendi. "
                    + "Demiryolu ve metro projeleri ulaşım yatırımları arasında sayıldı.";
            case "Çevre" -> "İklim değişikliği ve sürdürülebilirlik raporu kamuoyuyla paylaşıldı. Orman yangını riskine karşı önlemler anlatıldı. "
                    + "Geri dönüşüm ve atık yönetimi çevre politikalarının parçası.";
            case "Magazin" -> "Ünlü çift düğün hazırlıklarını magazin sayfalarına yansıttı. Film galası ve konser gecesi sosyal medyada gündem oldu. "
                    + "Televizyon programları rating yarışını sürdürüyor.";
            case "Kültür-Sanat" -> "Müze ve sergi açılışları kültür sanat takvimine eklendi. Tiyatro festivali biletleri satışa çıktı. "
                    + "Edebiyat ödülleri ve sinema eleştirmenleri yeni yapımları değerlendirdi.";
            case "Bilim-Teknoloji" -> "Uzay ajansı görev takvimini bilim camiasıyla paylaştı. Araştırma üniversiteleri laboratuvar bulgularını yayımladı. "
                    + "İklim modellemesi ve veri analizi bilim teknoloji haberlerinde öne çıktı.";
            case "Dünya" -> "Birleşmiş Milletler toplantısında dünya gündemi ele alındı. Komşu ülkelerle diplomatik temaslar sürdü. "
                    + "Küresel ticaret ve enerji piyasaları uluslararası başlıklarda yer aldı.";
            case "Sosyal" -> "Sosyal medya platformları yeni politika güncellemelerini duyurdu. Toplumsal dayanışma kampanyaları geniş kitlelere ulaştı. "
                    + "STK'lar sosyal haklar konusunda açıklama yaptı.";
            case "Savaş" -> "Savunma bakanlığı operasyonlara ilişkin bilgilendirme paylaştı. Sınır ötesi güvenlik tartışmaları gündemde. "
                    + "Uluslararası gözlemciler ateşkes çağrılarını yineledi.";
            case "Diğer" -> "Genel gündemde çeşitli başlıklar öne çıktı. Yerel yönetim duyuruları ve kurumsal açıklamalar bir arada okundu. "
                    + "Kategori sınırına uymayan konular diğer başlığı altında toplandı.";
            default -> unknownCategoryBaseText(name);
            };
        }
        return base + "\n\n" + variationSuffix(label, contentVariant);
    }

    private static String unknownCategoryBaseText(String name) {
        return "Güncel gelişmeler: " + name + " başlığı altında yürütülen çalışmalar ve kamuoyuna yansıyan ayrıntılar. "
                + "İlgili kurumlar açıklama yayımladı; süreç takip ediliyor.";
    }

    private static String variationSuffix(String categoryName, int v) {
        String[] extras = {
                "Uzman yorumları gündemi şekillendiriyor.",
                "Yerel basın gelişmeleri sayfalarına taşıdı.",
                "Sosyal medyada paylaşım sayısı arttı.",
                "İlgili kurum ikinci bir duyuru yayımladı.",
                "Vatandaşlar konuya dikkat çekti.",
                "Analistler kısa vadeli beklentilerini paylaştı.",
                "Bölge temsilcileri değerlendirme yaptı.",
                "Kamuoyu araştırması sonuçları tartışılıyor."
        };
        return "Ek bağlam (" + categoryName + ", örnek " + (v + 1) + "): "
                + extras[Math.floorMod(v, extras.length)]
                + " Bu haber metni yapay zeka eğitim veri seti için kategori içi çeşitlilik sağlar.";
    }
}
