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

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;
import java.io.Serializable;
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
    private static void progress(String stage, String detail) {
        String d = (detail == null || detail.isBlank()) ? "" : (";" + detail.replaceAll("\\s+", "_"));
        System.out.println("SPARK_PROGRESS:" + stage + d);
        System.out.flush();
    }

    public static void main(String[] args) throws Exception {
        int exitCode = 0;
        try {
            run(args);
        } catch (Throwable t) {
            exitCode = 1;
            String msg = t.getClass().getSimpleName() + ": " + String.valueOf(t.getMessage());
            System.out.println("SPARK_TRAINING_RESULT:samples=0;reason=error;error=" + msg.replaceAll("\\s+", "_"));
        } finally {
            // Spark bazı arka plan thread'lerini açık bırakabiliyor; child process'in kesin kapanması için.
            System.exit(exitCode);
        }
    }

    private static void run(String[] args) throws Exception {
        long startNs = System.nanoTime();
        Map<String, String> p = parseArgs(args);
        progress("START", "args_parsed");

        String dbUrl = required(p, "dbUrl");
        String dbUser = required(p, "dbUser");
        String dbPassword = required(p, "dbPassword");

        String modelPath = required(p, "modelPath");
        String sparkMaster = p.getOrDefault("sparkMaster", "local[*]");
        String sparkIoCompressionCodec = p.getOrDefault("sparkIoCompressionCodec", "lzf");

        int ngramMin = Integer.parseInt(p.getOrDefault("ngramMin", "1"));
        int ngramMax = Integer.parseInt(p.getOrDefault("ngramMax", "2"));
        int numFeatures = Integer.parseInt(p.getOrDefault("numFeatures", "262144"));
        int minTrainingSamples = Integer.parseInt(p.getOrDefault("minTrainingSamples", "30"));
        int sparkMaxTrainingSamples = Integer.parseInt(p.getOrDefault("sparkMaxTrainingSamples", "300"));

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
                ORDER BY r.id
                LIMIT ? OFFSET ?
                """;
        progress("DB_READ_START", "query_active_labels_paged");
        int pageSize = 200;
        int offset = 0;
        int maxRowsToRead = sparkMaxTrainingSamples > 0
                ? Math.max(minTrainingSamples * 2, sparkMaxTrainingSamples * 3)
                : Integer.MAX_VALUE;

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            while (examples.size() < maxRowsToRead) {
                int fetchedInPage = 0;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, pageSize);
                    ps.setInt(2, offset);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            fetchedInPage++;
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
                            if (examples.size() >= maxRowsToRead) {
                                break;
                            }
                        }
                    }
                }
                if (fetchedInPage == 0) {
                    break;
                }
                offset += pageSize;
            }
        }
        progress("DB_READ_DONE", "examples=" + examples.size() + ";labels=" + categoryToIndex.size());

        if (examples.size() < minTrainingSamples) {
            System.out.println("SPARK_TRAINING_RESULT:samples=0;reason=not_enough_samples");
            return;
        }

        if (sparkMaxTrainingSamples > 0 && examples.size() > sparkMaxTrainingSamples) {
            Collections.shuffle(examples, new Random(evaluationSeed));
            examples = new ArrayList<>(examples.subList(0, sparkMaxTrainingSamples));
            progress("SAMPLE_CAP", "capped_examples=" + examples.size());
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
                .set("spark.ui.enabled", "false")
                .set("spark.default.parallelism", "1")
                .set("spark.sql.shuffle.partitions", "1")
                .set("spark.driver.host", "127.0.0.1")
                .set("spark.driver.bindAddress", "127.0.0.1")
                .set("spark.io.compression.codec", sparkIoCompressionCodec)
                .set("spark.shuffle.spill.compress", "true")
                .set("spark.shuffle.compress", "true");

        progress("SPARK_CONTEXT_START", "master=" + sparkMaster);
        JavaSparkContext jsc = new JavaSparkContext(conf);
        jsc.setLogLevel("ERROR");
        progress("SPARK_CONTEXT_READY", "ok");
        try {
            progress("FEATURE_BUILD_START", "numFeatures=" + numFeatures);
            HashingTF hashingTF = new HashingTF(Math.max(1, numFeatures));
            JavaRDD<LabeledDoc> trainRdd = jsc.parallelize(trainExamples).coalesce(1);
            JavaRDD<List<String>> trainTokens = trainRdd.map(e -> e.terms);
            JavaRDD<Vector> trainTf = hashingTF.transform(trainTokens);
            JavaRDD<LabeledPoint> trainPoints = trainTf.zip(trainRdd)
                    .map(t -> new LabeledPoint(t._2.label, t._1));

            progress("TRAIN_START", "train_examples=" + trainExamples.size());
            NaiveBayesModel model = NaiveBayes.train(trainPoints.rdd(), 1.0, "multinomial");
            progress("TRAIN_DONE", "ok");

            Double accuracy = null;
            Double weightedF1 = null;
            Double weightedPrecision = null;
            Double weightedRecall = null;

            if (!testExamples.isEmpty() && runEvaluationAfterTrain) {
                progress("EVAL_START", "test_examples=" + testExamples.size());
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
                progress("EVAL_DONE", "ok");
            }

            Path dir = Path.of(modelPath).toAbsolutePath();
            Path modelPathDir = dir.resolve("pipeline");
            Path labelsPath = dir.resolve("labels.txt");
            if (Files.exists(modelPathDir)) {
                try (Stream<Path> walk = Files.walk(modelPathDir)) {
                    walk.sorted(Comparator.reverseOrder()).forEach(rootPath -> {
                        try {
                            Files.deleteIfExists(rootPath);
                        } catch (Exception ignored) {
                        }
                    });
                }
            }
            Files.createDirectories(modelPathDir);

            progress("MODEL_SAVE_START", "path=" + modelPathDir);
            model.save(jsc.sc(), modelPathDir.toString());
            // labelNames dosyası: index sırasına göre yaz
            List<String> labels = Arrays.asList(indexToCategory);
            Files.write(labelsPath, labels);

            Path nbExportPath = dir.resolve("nb-export.json");
            Path nbExportTmp = dir.resolve("nb-export.json.tmp");
            NbModelExport nbExport = new NbModelExport(
                    model.labels(),
                    model.pi(),
                    model.theta(),
                    model.modelType(),
                    numFeatures);
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(nbExportTmp.toFile(), nbExport);
            try {
                Files.move(nbExportTmp, nbExportPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception ignored) {
                Files.move(nbExportTmp, nbExportPath, StandardCopyOption.REPLACE_EXISTING);
            }
            progress("NB_EXPORT_JSON", "path=" + nbExportPath.getFileName());

            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
            progress("DONE", "elapsedMs=" + elapsedMs);

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

    private static class LabeledDoc implements Serializable {
        private static final long serialVersionUID = 1L;

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

