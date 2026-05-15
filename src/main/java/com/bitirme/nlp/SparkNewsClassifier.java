package com.bitirme.nlp;

import com.bitirme.entity.News;
import com.bitirme.nlp.config.MlClassifierProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.mllib.classification.NaiveBayesModel;
import org.apache.spark.mllib.feature.HashingTF;
import org.apache.spark.mllib.linalg.Vector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Spark MLlib NaiveBayes modeli ile haber metnini sınıflandırır.
 * Model yoksa veya yüklenemezse null döner (keyword fallback kullanılır).
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ml.classifier", name = "enabled", havingValue = "true")
@Slf4j
public class SparkNewsClassifier {

    // Parquet/NaiveBayesModel.load ana JVM'de Spark SQL + Hibernate ANTLR çakışması yaratır; yalnızca nb-export.json.
    private final ObjectMapper objectMapper;
    private final TurkishTextPreprocessor preprocessor;
    private final MlClassifierProperties properties;

    private NaiveBayesModel model;
    private HashingTF hashingTF;
    private List<String> labelNames = Collections.emptyList();

    @PostConstruct
    public void init() {
        if (!properties.isEnabled()) {
            log.info("ML classifier is disabled.");
            return;
        }
        try {
            loadModel();
        } catch (Throwable t) {
            // SparkContext yüklenirken/başlatılırken hata alırsak uygulamanın ayağa kalkmasını engellememeliyiz.
            log.error("SparkNewsClassifier init failed, continue without Spark model.", t);
            model = null;
            hashingTF = null;
            labelNames = Collections.emptyList();
        }
    }

    /**
     * Model dizininden PipelineModel ve label listesini yükler.
     */
    public void loadModel() {
        Path dir = Path.of(properties.getModelPath()).toAbsolutePath().normalize();
        Path labelsPath = dir.resolve("labels.txt");
        Path exportPath = dir.resolve("nb-export.json");

        if (!Files.exists(labelsPath)) {
            log.warn("labels.txt missing under {}. Using keyword fallback.", dir);
            model = null;
            hashingTF = null;
            labelNames = Collections.emptyList();
            return;
        }

        // Önce nb-export.json: parquet/SQL yolu NaiveBayesModel.load → Hibernate ile ANTLR çakışması
        if (Files.exists(exportPath)) {
            try {
                NbModelExport exp = objectMapper.readValue(exportPath.toFile(), NbModelExport.class);
                String mt = exp.modelType() != null && !exp.modelType().isBlank()
                        ? exp.modelType()
                        : "multinomial";
                model = new NaiveBayesModel(exp.labels(), exp.pi(), exp.theta(), mt);
                int nf = exp.numFeatures() > 0
                        ? exp.numFeatures()
                        : (properties.getNumFeatures() > 0 ? properties.getNumFeatures() : (1 << 18));
                hashingTF = new HashingTF(Math.max(1, nf));
                labelNames = Files.readAllLines(labelsPath);
                if (labelNames.isEmpty()) {
                    log.warn("labels.txt is empty.");
                    model = null;
                    hashingTF = null;
                } else {
                    log.info("ML model loaded from nb-export.json (Spark SQL yok). {} categories.", labelNames.size());
                }
                return;
            } catch (Throwable e) {
                log.warn("nb-export.json okunamadı: {}", e.toString());
            }
        }

        log.warn(
                "nb-export.json yok veya geçersiz; ana JVM'de parquet yüklenmez (ANTLR/Spark SQL). "
                        + "POST /api/ml/train ile nb-export.json oluşturun. modelDir={}",
                dir);
        model = null;
        hashingTF = null;
        labelNames = Collections.emptyList();
    }

    /**
     * Haberi ML modeli ile sınıflandırır. Model yoksa veya hata olursa empty döner.
     */
    public Optional<MlClassificationResult> classify(News news) {
        if (model == null || hashingTF == null || labelNames.isEmpty()) {
            return Optional.empty();
        }
        String text = preprocessor.preprocess(news.getTitle(), news.getContent());
        if (text.isBlank()) {
            return Optional.empty();
        }
        try {
            List<String> tokens = tokenize(text);
            List<String> terms = expandNGrams(tokens, Math.max(properties.getNGramMin(), properties.getNGramMax()));
            if (terms.isEmpty()) return Optional.empty();
            Vector features = hashingTF.transform(terms);
            double predictedLabel = model.predict(features);
            double[] ls = model.labels();
            int idx = Arrays.binarySearch(ls, predictedLabel);
            if (idx < 0 || idx >= labelNames.size()) {
                return Optional.of(MlClassificationResult.of("Diğer", 0.1));
            }
            String categoryName = labelNames.get(idx);
            // mllib NaiveBayesModel doğrudan olasılık döndürmediği için sabit güven.
            double confidence = 0.85;

            if (confidence < properties.getMinConfidence()) {
                categoryName = "Diğer";
            }
            return Optional.of(MlClassificationResult.of(categoryName, confidence));
        } catch (Exception e) {
            log.debug("ML classification failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public boolean isModelLoaded() {
        return model != null && !labelNames.isEmpty();
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.stream(text.split("\\s+"))
                .filter(t -> !t.isBlank())
                .toList();
    }

    private List<String> expandNGrams(List<String> tokens, int nGramMax) {
        if (tokens.isEmpty()) return List.of();
        int maxN = Math.max(1, Math.min(3, nGramMax));
        List<String> terms = new ArrayList<>(tokens);
        for (int n = 2; n <= maxN; n++) {
            for (int i = 0; i + n <= tokens.size(); i++) {
                terms.add(String.join("_", tokens.subList(i, i + n)));
            }
        }
        return terms;
    }
}
