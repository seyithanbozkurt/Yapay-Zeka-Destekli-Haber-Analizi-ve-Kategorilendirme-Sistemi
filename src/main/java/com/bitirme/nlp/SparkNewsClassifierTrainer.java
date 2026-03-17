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
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator;
import org.apache.spark.ml.feature.HashingTF;
import org.apache.spark.ml.feature.IDF;
import org.apache.spark.ml.feature.NGram;
import org.apache.spark.ml.feature.SQLTransformer;
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
 * Spark ML pipeline: Metin ön işleme → Tokenizer → N-gram (unigram+bigram+trigram) →
 * TF-IDF (Bag of Words + kelime frekansları) → Özellik vektörü → Linear SVM (One-vs-Rest).
 * Eğitim sonrası isteğe bağlı Accuracy, Precision, Recall, F1 değerlendirmesi yapar.
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

        Dataset<Row> trainData;
        Dataset<Row> testData = null;
        if (properties.isRunEvaluationAfterTrain() && properties.getTestSplitRatio() > 0 && properties.getTestSplitRatio() < 1.0) {
            Dataset<Row>[] splits = df.randomSplit(new double[]{1.0 - properties.getTestSplitRatio(), properties.getTestSplitRatio()}, properties.getEvaluationSeed());
            trainData = splits[0];
            testData = splits[1];
            if (testData.count() < 5) {
                testData = null;
                trainData = df;
            }
        } else {
            trainData = df;
        }

        List<PipelineStage> stages = new ArrayList<>();

        Tokenizer tokenizer = new Tokenizer()
                .setInputCol("text")
                .setOutputCol("words");
        stages.add(tokenizer);

        String featureInputCol = "words";
        int nGramMax = Math.max(properties.getNGramMin(), Math.min(properties.getNGramMax(), 3));
        if (nGramMax >= 2) {
            NGram bigram = new NGram().setN(2).setInputCol("words").setOutputCol("bigrams");
            stages.add(bigram);
            if (nGramMax >= 3) {
                NGram trigram = new NGram().setN(3).setInputCol("words").setOutputCol("trigrams");
                stages.add(trigram);
                SQLTransformer mergeNgrams = new SQLTransformer()
                        .setStatement("SELECT *, array_union(array_union(words, bigrams), trigrams) AS allTerms FROM __THIS__");
                stages.add(mergeNgrams);
                featureInputCol = "allTerms";
            } else {
                SQLTransformer mergeNgrams = new SQLTransformer()
                        .setStatement("SELECT *, array_union(words, bigrams) AS allTerms FROM __THIS__");
                stages.add(mergeNgrams);
                featureInputCol = "allTerms";
            }
        }

        HashingTF hashingTF = new HashingTF()
                .setInputCol(featureInputCol)
                .setOutputCol("rawFeatures")
                .setNumFeatures(properties.getNumFeatures() > 0 ? properties.getNumFeatures() : (1 << 18));

        IDF idf = new IDF()
                .setInputCol("rawFeatures")
                .setOutputCol("features");

        LinearSVC linearSVC = new LinearSVC()
                .setFeaturesCol("features")
                .setLabelCol("label")
                .setPredictionCol("prediction")
                .setMaxIter(properties.getMaxIter())
                .setRegParam(properties.getRegParam());

        OneVsRest ovr = new OneVsRest()
                .setClassifier(linearSVC)
                .setLabelCol("label")
                .setFeaturesCol("features")
                .setPredictionCol("prediction");

        stages.add(hashingTF);
        stages.add(idf);
        stages.add(ovr);

        Pipeline pipeline = new Pipeline().setStages(stages.toArray(new PipelineStage[0]));
        PipelineModel model = pipeline.fit(trainData);

        if (testData != null && properties.isRunEvaluationAfterTrain()) {
            try {
                Dataset<Row> predictions = model.transform(testData);
                lastEvaluationMetrics = computeMetrics(predictions, categoryNames);
                log.info("Evaluation - Accuracy: {}, Weighted F1: {}", lastEvaluationMetrics.getAccuracy(), lastEvaluationMetrics.getWeightedF1());
            } catch (Exception ex) {
                log.warn("Evaluation failed: {}", ex.getMessage());
            }
        }

        Path dir = Path.of(properties.getModelPath());
        Path pipelinePath = dir.resolve("pipeline");
        Path labelsPath = dir.resolve("labels.txt");
        try {
            Files.createDirectories(pipelinePath);
            model.save(pipelinePath.toString());
            Files.write(labelsPath, categoryNames);
            log.info("Model saved. Samples: {}, Categories: {}, N-gram max: {}", rows.size(), categoryNames.size(), nGramMax);
        } catch (Exception e) {
            log.error("Failed to save model: {}", e.getMessage());
            return 0;
        }
        return rows.size();
    }

    private MlEvaluationMetrics computeMetrics(Dataset<Row> predictions, List<String> categoryNames) {
        MulticlassClassificationEvaluator accEvaluator = new MulticlassClassificationEvaluator()
                .setLabelCol("label").setPredictionCol("prediction").setMetricName("accuracy");
        MulticlassClassificationEvaluator f1Evaluator = new MulticlassClassificationEvaluator()
                .setLabelCol("label").setPredictionCol("prediction").setMetricName("f1");
        MulticlassClassificationEvaluator precEvaluator = new MulticlassClassificationEvaluator()
                .setLabelCol("label").setPredictionCol("prediction").setMetricName("weightedPrecision");
        MulticlassClassificationEvaluator recEvaluator = new MulticlassClassificationEvaluator()
                .setLabelCol("label").setPredictionCol("prediction").setMetricName("weightedRecall");

        double accuracy = accEvaluator.evaluate(predictions);
        double weightedF1 = f1Evaluator.evaluate(predictions);
        double weightedPrecision = precEvaluator.evaluate(predictions);
        double weightedRecall = recEvaluator.evaluate(predictions);

        return MlEvaluationMetrics.builder()
                .accuracy(accuracy)
                .weightedPrecision(weightedPrecision)
                .weightedRecall(weightedRecall)
                .weightedF1(weightedF1)
                .macroF1(weightedF1)
                .testSampleCount((int) predictions.count())
                .build();
    }

    public MlEvaluationMetrics getLastEvaluationMetrics() {
        return lastEvaluationMetrics;
    }
}
