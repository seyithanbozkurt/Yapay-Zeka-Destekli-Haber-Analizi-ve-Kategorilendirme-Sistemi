package com.bitirme.nlp;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.classification.NaiveBayes;
import org.apache.spark.mllib.classification.NaiveBayesModel;
import org.apache.spark.mllib.evaluation.MulticlassMetrics;
import org.apache.spark.mllib.feature.HashingTF;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.regression.LabeledPoint;
import scala.Tuple2;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * Spring uygulamasından ayrı JVM'de Spark eğitim çalıştırır.
 * Amaç: Hibernate ile Spark arasında ANTLR runtime çakışmasını classpath izolasyonu ile kırmak.
 */
public class SparkStandaloneTrainerMain {

    public static void main(String[] args) throws Exception {
        Map<String, String> p = parseArgs(args);

        String dbUrl = required(p, "dbUrl");
        String dbUser = required(p, "dbUser");
        String dbPassword = required(p, "dbPassword");

        String modelPath = required(p, "modelPath");
        String sparkMaster = p.getOrDefault("sparkMaster", "local[*]");

        int ngramMin = Integer.parseInt(p.getOrDefault("ngramMin", "1"));
        int ngramMax = Integer.parseInt(p.getOrDefault("ngramMax", "2"));
        int numFeatures = Integer.parseInt(p.getOrDefault("numFeatures", "262144"));
        int minTrainingSamples = Integer.parseInt(p.getOrDefault("minTrainingSamples", "30"));

        boolean runEvaluationAfterTrain = Boolean.parseBoolean(p.getOrDefault("runEvaluationAfterTrain", "true"));
        double testSplitRatio = Double.parseDouble(p.getOrDefault("testSplitRatio", "0.2"));
        long evaluationSeed = Long.parseLong(p.getOrDefault("evaluationSeed", "42"));

        TurkishTextPreprocessor preprocessor = new TurkishTextPreprocessor();

        // Labeled sample listesi: (labelIndex, terms)
        List<LabeledDoc> examples = new ArrayList<>();
        Map<String, Integer> categoryToIndex = new HashMap<>();

        String sql = """
                SELECT n.title, n.content, c.name
                FROM news_classification_results r
                JOIN news n ON r.news_id = n.id
                JOIN categories c ON r.predicted_category_id = c.id
                WHERE r.is_active = true
                """;

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String title = rs.getString(1);
                String content = rs.getString(2);
                String categoryName = rs.getString(3);
                if (categoryName == null || categoryName.isBlank()) continue;

                int labelIndex = categoryToIndex.computeIfAbsent(categoryName, k -> categoryToIndex.size());

                String text = preprocessor.preprocess(title, content);
                if (text == null || text.isBlank()) continue;

                List<String> tokens = tokenize(text);
                int maxN = Math.max(ngramMin, ngramMax);
                List<String> terms = expandNGrams(tokens, maxN);
                if (terms.isEmpty()) continue;

                examples.add(new LabeledDoc((double) labelIndex, terms, categoryName));
            }
        }

        if (examples.size() < minTrainingSamples) {
            System.out.println("SPARK_TRAINING_RESULT:samples=0;reason=not_enough_samples");
            return;
        }

        // labelNames (index sırası -> category adı) stable olsun diye index -> name map.
        String[] indexToCategory = new String[categoryToIndex.size()];
        for (Map.Entry<String, Integer> e : categoryToIndex.entrySet()) {
            indexToCategory[e.getValue()] = e.getKey();
        }

        Collections.shuffle(examples, new Random(evaluationSeed));
        int splitIndex = (int) Math.max(1, Math.floor(examples.size() * (1.0 - testSplitRatio)));
        List<LabeledDoc> trainExamples = examples;
        List<LabeledDoc> testExamples = List.of();
        if (runEvaluationAfterTrain && testSplitRatio > 0 && testSplitRatio < 1.0 && examples.size() > 10) {
            trainExamples = examples.subList(0, splitIndex);
            testExamples = examples.subList(splitIndex, examples.size());
        }

        SparkConf conf = new SparkConf()
                .setAppName("SparkStandaloneTrainer")
                .setMaster(sparkMaster)
                .set("spark.ui.enabled", "false");

        JavaSparkContext jsc = new JavaSparkContext(conf);
        try {
            HashingTF hashingTF = new HashingTF(Math.max(1, numFeatures));
            JavaRDD<LabeledDoc> trainRdd = jsc.parallelize(trainExamples);
            JavaRDD<List<String>> trainTokens = trainRdd.map(e -> e.terms);
            JavaRDD<Vector> trainTf = hashingTF.transform(trainTokens);
            JavaRDD<LabeledPoint> trainPoints = trainTf.zip(trainRdd)
                    .map(t -> new LabeledPoint(t._2.label, t._1));

            NaiveBayesModel model = NaiveBayes.train(trainPoints.rdd(), 1.0, "multinomial");

            Double accuracy = null;
            Double weightedF1 = null;
            Double weightedPrecision = null;
            Double weightedRecall = null;

            if (!testExamples.isEmpty() && runEvaluationAfterTrain) {
                JavaRDD<LabeledDoc> testRdd = jsc.parallelize(testExamples);
                JavaRDD<List<String>> testTokens = testRdd.map(e -> e.terms);
                JavaRDD<Vector> testTf = hashingTF.transform(testTokens);
                JavaRDD<LabeledPoint> testPoints = testTf.zip(testRdd)
                        .map(t -> new LabeledPoint(t._2.label, t._1));

                JavaRDD<Tuple2<Object, Object>> predictionAndLabels = testPoints.map(
                        lp -> new Tuple2<>(model.predict(lp.features()), lp.label())
                );

                MulticlassMetrics metrics = new MulticlassMetrics(predictionAndLabels.rdd());
                accuracy = metrics.accuracy();
                weightedF1 = metrics.weightedFMeasure();
                weightedPrecision = metrics.weightedPrecision();
                weightedRecall = metrics.weightedRecall();
            }

            Path dir = Path.of(modelPath).toAbsolutePath();
            Path modelPathDir = dir.resolve("pipeline");
            Path labelsPath = dir.resolve("labels.txt");
            Files.createDirectories(modelPathDir);

            model.save(jsc.sc(), modelPathDir.toString());
            // labelNames dosyası: index sırasına göre yaz
            List<String> labels = Arrays.asList(indexToCategory);
            Files.write(labelsPath, labels);

            System.out.println(
                    "SPARK_TRAINING_RESULT:samples=" + examples.size() +
                            ";labels=" + labels.size() +
                            ";accuracy=" + (accuracy != null ? accuracy : "null") +
                            ";weightedF1=" + (weightedF1 != null ? weightedF1 : "null") +
                            ";weightedPrecision=" + (weightedPrecision != null ? weightedPrecision : "null") +
                            ";weightedRecall=" + (weightedRecall != null ? weightedRecall : "null")
            );
        } finally {
            jsc.close();
        }
    }

    private static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.stream(text.split("\\s+"))
                .filter(t -> !t.isBlank())
                .toList();
    }

    private static List<String> expandNGrams(List<String> tokens, int nGramMax) {
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

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> m = new HashMap<>();
        for (String a : args) {
            if (!a.contains("=")) continue;
            String[] kv = a.split("=", 2);
            m.put(kv[0].replaceFirst("^--", ""), kv[1]);
        }
        return m;
    }

    private static String required(Map<String, String> p, String key) {
        String v = p.get(key);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("Missing arg: " + key);
        }
        return v;
    }

    private static class LabeledDoc {
        final double label;
        final List<String> terms;
        @SuppressWarnings("unused")
        final String categoryName;

        private LabeledDoc(double label, List<String> terms, String categoryName) {
            this.label = label;
            this.terms = terms;
            this.categoryName = categoryName;
        }
    }
}

