package com.bitirme.nlp;

import com.bitirme.entity.News;
import com.bitirme.entity.NewsClassificationResult;
import com.bitirme.nlp.config.MlClassifierProperties;
import com.bitirme.repository.NewsClassificationResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.classification.LinearSVC;
import org.apache.spark.ml.classification.OneVsRest;
import org.apache.spark.ml.feature.HashingTF;
import org.apache.spark.ml.feature.IDF;
import org.apache.spark.ml.feature.Tokenizer;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Veritabanındaki etiketli haberlerle Spark ML pipeline (TF-IDF + Naive Bayes) eğitir ve kaydeder.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ml.classifier", name = "enabled", havingValue = "true")
@Slf4j
public class SparkNewsClassifierTrainer {

    private final SparkSession sparkSession;
    private final TurkishTextPreprocessor preprocessor;
    private final MlClassifierProperties properties;
    private final NewsClassificationResultRepository classificationResultRepository;

    /**
     * Etiketli haberleri kullanarak pipeline eğitir ve modelPath'e yazar.
     * @return eğitilen örnek sayısı; yetersiz veri varsa 0
     */
    public int trainAndSave() {
        List<NewsClassificationResult> results = classificationResultRepository.findByActiveTrue();
        if (results.isEmpty()) {
            log.warn("No classification results found for training.");
            return 0;
        }

        // Haber bazında tek etiket (ilk sınıflandırma sonucu) ve News referansı
        Map<Long, String> newsIdToCategoryName = new LinkedHashMap<>();
        Map<Long, News> newsById = new HashMap<>();
        for (NewsClassificationResult r : results) {
            newsIdToCategoryName.putIfAbsent(r.getNews().getId(), r.getPredictedCategory().getName());
            newsById.putIfAbsent(r.getNews().getId(), r.getNews());
        }

        List<String> categoryNames = new ArrayList<>(new TreeSet<>(newsIdToCategoryName.values()));
        Map<String, Integer> categoryToIndex = new HashMap<>();
        for (int i = 0; i < categoryNames.size(); i++) {
            categoryToIndex.put(categoryNames.get(i), i);
        }

        if (newsIdToCategoryName.size() < properties.getMinTrainingSamples()) {
            log.warn("Not enough training samples: {} (min {}). Skipping training.",
                    newsIdToCategoryName.size(), properties.getMinTrainingSamples());
            return 0;
        }

        List<Row> rows = new ArrayList<>();
        for (Map.Entry<Long, String> e : newsIdToCategoryName.entrySet()) {
            News news = newsById.get(e.getKey());
            if (news == null) continue;
            String text = preprocessor.preprocess(news.getTitle(), news.getContent());
            if (text.isBlank()) continue;
            int labelIndex = categoryToIndex.getOrDefault(e.getValue(), 0);
            rows.add(RowFactory.create(text, (double) labelIndex));
        }

        if (rows.size() < properties.getMinTrainingSamples()) {
            log.warn("After preprocessing, not enough samples: {} (min {}).", rows.size(), properties.getMinTrainingSamples());
            return 0;
        }

        StructType schema = new StructType(new StructField[]{
                new StructField("text", DataTypes.StringType, false, Metadata.empty()),
                new StructField("label", DataTypes.DoubleType, false, Metadata.empty())
        });
        Dataset<Row> df = sparkSession.createDataFrame(rows, schema);

        Tokenizer tokenizer = new Tokenizer()
                .setInputCol("text")
                .setOutputCol("words");

        HashingTF hashingTF = new HashingTF()
                .setInputCol("words")
                .setOutputCol("rawFeatures")
                .setNumFeatures(1 << 18);

        IDF idf = new IDF()
                .setInputCol("rawFeatures")
                .setOutputCol("features");

        // Lineer SVM tabanlı çok sınıflı sınıflandırıcı (One-vs-Rest)
        LinearSVC linearSVC = new LinearSVC()
                .setFeaturesCol("features")
                .setLabelCol("label")
                .setPredictionCol("prediction")
                .setMaxIter(100)
                .setRegParam(0.1);

        OneVsRest ovr = new OneVsRest()
                .setClassifier(linearSVC)
                .setLabelCol("label")
                .setFeaturesCol("features")
                .setPredictionCol("prediction");

        Pipeline pipeline = new Pipeline().setStages(new PipelineStage[]{tokenizer, hashingTF, idf, ovr});
        PipelineModel model = pipeline.fit(df);

        Path dir = Path.of(properties.getModelPath());
        Path pipelinePath = dir.resolve("pipeline");
        Path labelsPath = dir.resolve("labels.txt");
        try {
            Files.createDirectories(pipelinePath);
            model.save(pipelinePath.toString());
            Files.write(labelsPath, categoryNames);
            log.info("Model saved. Samples: {}, Categories: {}", rows.size(), categoryNames.size());
        } catch (Exception e) {
            log.error("Failed to save model: {}", e.getMessage());
            return 0;
        }
        return rows.size();
    }
}
