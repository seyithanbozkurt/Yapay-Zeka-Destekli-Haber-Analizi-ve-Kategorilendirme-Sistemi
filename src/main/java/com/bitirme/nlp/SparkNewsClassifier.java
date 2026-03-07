package com.bitirme.nlp;

import com.bitirme.entity.News;
import com.bitirme.nlp.config.MlClassifierProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Spark ML PipelineModel ile haber metnini sınıflandırır.
 * Model yoksa veya yüklenemezse null döner (keyword fallback kullanılır).
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ml.classifier", name = "enabled", havingValue = "true")
@Slf4j
public class SparkNewsClassifier {

    private final SparkSession sparkSession;
    private final TurkishTextPreprocessor preprocessor;
    private final MlClassifierProperties properties;

    private PipelineModel pipelineModel;
    private List<String> labelNames = Collections.emptyList();

    @PostConstruct
    public void init() {
        if (!properties.isEnabled()) {
            log.info("ML classifier is disabled.");
            return;
        }
        loadModel();
    }

    /**
     * Model dizininden PipelineModel ve label listesini yükler.
     */
    public void loadModel() {
        Path dir = Path.of(properties.getModelPath());
        Path modelPath = dir.resolve("pipeline");
        Path labelsPath = dir.resolve("labels.txt");
        if (!Files.isDirectory(modelPath) || !Files.exists(labelsPath)) {
            log.warn("ML model not found at {} or labels.txt missing. Using keyword fallback.", modelPath);
            return;
        }
        try {
            pipelineModel = PipelineModel.load(modelPath.toString());
            labelNames = Files.readAllLines(labelsPath);
            if (labelNames.isEmpty()) {
                log.warn("labels.txt is empty.");
                pipelineModel = null;
            } else {
                log.info("ML model loaded. {} categories.", labelNames.size());
            }
        } catch (Exception e) {
            log.error("Failed to load ML model from {}: {}", modelPath, e.getMessage());
            pipelineModel = null;
        }
    }

    /**
     * Haberi ML modeli ile sınıflandırır. Model yoksa veya hata olursa empty döner.
     */
    public Optional<MlClassificationResult> classify(News news) {
        if (pipelineModel == null || labelNames.isEmpty()) {
            return Optional.empty();
        }
        String text = preprocessor.preprocess(news.getTitle(), news.getContent());
        if (text.isBlank()) {
            return Optional.empty();
        }
        try {
            StructType schema = new StructType(new StructField[]{
                    new StructField("text", DataTypes.StringType, false, Metadata.empty())
            });
            Row row = RowFactory.create(text);
            Dataset<Row> df = sparkSession.createDataFrame(Collections.singletonList(row), schema);
            Dataset<Row> predicted = pipelineModel.transform(df);
            Row first = predicted.first();
            if (first == null) return Optional.empty();

            int predIndex = (int) first.getDouble(first.fieldIndex("prediction"));
            if (predIndex < 0 || predIndex >= labelNames.size()) {
                return Optional.of(MlClassificationResult.of("Diğer", 0.1));
            }
            String categoryName = labelNames.get(predIndex);

            double confidence = 0.5;
            try {
                Object prob = first.get(first.fieldIndex("probability"));
                if (prob != null && prob instanceof org.apache.spark.ml.linalg.Vector) {
                    org.apache.spark.ml.linalg.Vector vec = (org.apache.spark.ml.linalg.Vector) prob;
                    confidence = vec.toArray()[predIndex];
                }
            } catch (Exception ignored) {}

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
        return pipelineModel != null && !labelNames.isEmpty();
    }
}
