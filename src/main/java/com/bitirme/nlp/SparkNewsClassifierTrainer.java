package com.bitirme.nlp;

import com.bitirme.entity.News;
import com.bitirme.entity.NewsClassificationResult;
import com.bitirme.nlp.config.MlClassifierProperties;
import com.bitirme.repository.NewsClassificationResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.classification.NaiveBayes;
import org.apache.spark.mllib.classification.NaiveBayesModel;
import org.apache.spark.mllib.evaluation.MulticlassMetrics;
import org.apache.spark.mllib.feature.HashingTF;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import scala.Tuple2;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Spark MLlib: Metin ön işleme + N-gram + HashingTF + NaiveBayes.
 * Spark 3.5'te {@code NaiveBayes.train} iç yolu Catalyst/SQL tarafına düşebildiği için
 * (SqlBaseLexer) Hibernate ile aynı JVM'de ANTLR runtime çakışması oluşabilir; bu durumda
 * izole process ({@code SparkStandaloneTrainerMain}) kullanılır.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ml.classifier", name = "enabled", havingValue = "true")
@Slf4j
public class SparkNewsClassifierTrainer {

    // SparkContext'i sadece eğitim anında deneyelim (startup'ta patlamasın).
    private final ObjectProvider<JavaSparkContext> sparkContextProvider;
    private final TurkishTextPreprocessor preprocessor;
    private final MlClassifierProperties properties;
    private final NewsClassificationResultRepository classificationResultRepository;

    private volatile MlEvaluationMetrics lastEvaluationMetrics;

    /**
     * Etiketli haberlerle pipeline eğitir, diske yazar; isteğe bağlı train/test split ile değerlendirme yapar.
     * @return eğitilen örnek sayısı; yetersiz veri varsa 0
     */
    public int trainAndSave() {
        lastEvaluationMetrics = null;
        List<NewsClassificationResult> results = classificationResultRepository.findByActiveTrue();
        if (results.isEmpty()) {
            log.warn("No classification results found for training.");
            return 0;
        }

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

        List<LabeledExample> examples = new ArrayList<>();
        for (Map.Entry<Long, String> e : newsIdToCategoryName.entrySet()) {
            News news = newsById.get(e.getKey());
            if (news == null) continue;
            String text = preprocessor.preprocess(news.getTitle(), news.getContent());
            if (text.isBlank()) continue;
            int labelIndex = categoryToIndex.getOrDefault(e.getValue(), 0);
            List<String> tokens = tokenize(text);
            List<String> terms = expandNGrams(tokens, Math.max(properties.getNGramMin(), properties.getNGramMax()));
            if (terms.isEmpty()) continue;
            examples.add(new LabeledExample((double) labelIndex, terms));
        }

        if (examples.size() < properties.getMinTrainingSamples()) {
            log.warn("After preprocessing, not enough samples: {} (min {}).", examples.size(), properties.getMinTrainingSamples());
            return 0;
        }

        JavaSparkContext jsc = sparkContextProvider.getIfAvailable();
        if (jsc == null) {
            throw new IllegalStateException("SparkContext unavailable; cannot train Spark model.");
        }

        List<LabeledExample> trainExamples = examples;
        List<LabeledExample> testExamples = new ArrayList<>();
        if (properties.isRunEvaluationAfterTrain() && properties.getTestSplitRatio() > 0 && properties.getTestSplitRatio() < 1.0) {
            Collections.shuffle(examples, new Random(properties.getEvaluationSeed()));
            int splitIndex = (int) Math.max(1, Math.floor(examples.size() * (1.0 - properties.getTestSplitRatio())));
            trainExamples = new ArrayList<>(examples.subList(0, splitIndex));
            testExamples = new ArrayList<>(examples.subList(splitIndex, examples.size()));
            if (testExamples.size() < 5) {
                testExamples = new ArrayList<>();
                trainExamples = examples;
            }
        }

        HashingTF hashingTF = new HashingTF(properties.getNumFeatures() > 0 ? properties.getNumFeatures() : (1 << 18));
        JavaRDD<List<String>> trainTokens = jsc.parallelize(trainExamples).map(e -> e.terms);
        JavaRDD<Vector> trainTf = hashingTF.transform(trainTokens);
        JavaRDD<LabeledPoint> trainPoints = trainTf.zip(jsc.parallelize(trainExamples))
                .map(t -> new LabeledPoint(t._2.label, t._1));

        NaiveBayesModel model = NaiveBayes.train(trainPoints.rdd(), 1.0, "multinomial");

        if (!testExamples.isEmpty() && properties.isRunEvaluationAfterTrain()) {
            try {
                JavaRDD<List<String>> testTokens = jsc.parallelize(testExamples).map(e -> e.terms);
                JavaRDD<Vector> testTf = hashingTF.transform(testTokens);
                JavaRDD<LabeledPoint> testPoints = testTf.zip(jsc.parallelize(testExamples))
                        .map(t -> new LabeledPoint(t._2.label, t._1));
                JavaRDD<Tuple2<Object, Object>> predictionAndLabels = testPoints.map(
                        p -> new Tuple2<>(model.predict(p.features()), p.label())
                );
                lastEvaluationMetrics = computeMetrics(predictionAndLabels);
                log.info("Evaluation - Accuracy: {}, Weighted F1: {}", lastEvaluationMetrics.getAccuracy(), lastEvaluationMetrics.getWeightedF1());
            } catch (Exception ex) {
                log.warn("Evaluation failed: {}", ex.getMessage());
            }
        }

        Path dir = Path.of(properties.getModelPath());
        Path modelPath = dir.resolve("pipeline");
        Path labelsPath = dir.resolve("labels.txt");
        try {
            Files.createDirectories(modelPath);
            model.save(jsc.sc(), modelPath.toString());
            Files.write(labelsPath, categoryNames);
            log.info("Spark MLlib model saved. Samples: {}, Categories: {}", examples.size(), categoryNames.size());
        } catch (Exception e) {
            log.error("Failed to save model: {}", e.getMessage());
            return 0;
        }
        return examples.size();
    }

    private MlEvaluationMetrics computeMetrics(JavaRDD<Tuple2<Object, Object>> predictionAndLabels) {
        MulticlassMetrics metrics = new MulticlassMetrics(predictionAndLabels.rdd());
        double accuracy = metrics.accuracy();
        double weightedF1 = metrics.weightedFMeasure();
        double weightedPrecision = metrics.weightedPrecision();
        double weightedRecall = metrics.weightedRecall();

        return MlEvaluationMetrics.builder()
                .accuracy(accuracy)
                .weightedPrecision(weightedPrecision)
                .weightedRecall(weightedRecall)
                .weightedF1(weightedF1)
                .macroF1(weightedF1)
                .testSampleCount((int) predictionAndLabels.count())
                .build();
    }

    public MlEvaluationMetrics getLastEvaluationMetrics() {
        return lastEvaluationMetrics;
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

    private static class LabeledExample {
        final double label;
        final List<String> terms;

        private LabeledExample(double label, List<String> terms) {
            this.label = label;
            this.terms = terms;
        }
    }
}
