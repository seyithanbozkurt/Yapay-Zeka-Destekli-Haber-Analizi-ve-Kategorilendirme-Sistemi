package com.bitirme.service;

import com.bitirme.nlp.MlEvaluationMetrics;
import com.bitirme.nlp.NaiveBayesNewsClassifier;
import com.bitirme.nlp.SparkNewsClassifier;
import com.bitirme.nlp.SparkNewsClassifierTrainer;
import com.bitirme.nlp.config.MlClassifierProperties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Spark + Naive Bayes tam eğitim akışı (controller'dan çağrılır; senkron veya arka planda).
 */
@Service
@Slf4j
public class MlModelTrainingService {

    private final MlClassifierProperties mlProperties;
    private final Optional<SparkNewsClassifierTrainer> sparkTrainer;
    private final Optional<SparkNewsClassifier> sparkClassifier;
    private final Optional<NaiveBayesNewsClassifier> naiveBayesClassifier;
    private final Executor mlTrainingExecutor;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    private final AtomicBoolean trainingInProgress = new AtomicBoolean(false);

    @Getter
    private volatile TrainingSnapshot lastTrainingSnapshot;

    public MlModelTrainingService(
            MlClassifierProperties mlProperties,
            Optional<SparkNewsClassifierTrainer> sparkTrainer,
            Optional<SparkNewsClassifier> sparkClassifier,
            Optional<NaiveBayesNewsClassifier> naiveBayesClassifier,
            @Qualifier("mlTrainingExecutor") Executor mlTrainingExecutor
    ) {
        this.mlProperties = mlProperties;
        this.sparkTrainer = sparkTrainer;
        this.sparkClassifier = sparkClassifier;
        this.naiveBayesClassifier = naiveBayesClassifier;
        this.mlTrainingExecutor = mlTrainingExecutor;
    }

    public boolean isTrainingInProgress() {
        return trainingInProgress.get();
    }

    /**
     * @return true iş kuyruğa alındı; false zaten eğitim çalışıyor
     */
    public boolean tryRunTrainingAsync(Runnable onComplete) {
        if (!trainingInProgress.compareAndSet(false, true)) {
            return false;
        }
        CompletableFuture.runAsync(() -> {
            try {
                TrainingOutcome outcome = runFullTrainingSync();
                lastTrainingSnapshot = new TrainingSnapshot(Instant.now(), outcome.message(), outcome.data(), null);
                log.info("ML eğitim tamamlandı: {}", outcome.message());
            } catch (Throwable t) {
                log.error("ML eğitim hatası", t);
                Map<String, Object> err = new HashMap<>();
                err.put("error", t.getClass().getSimpleName() + ": " + t.getMessage());
                lastTrainingSnapshot = new TrainingSnapshot(Instant.now(), null, err, t);
            } finally {
                trainingInProgress.set(false);
                if (onComplete != null) {
                    try {
                        onComplete.run();
                    } catch (Throwable ignored) {
                    }
                }
            }
        }, mlTrainingExecutor);
        return true;
    }

    public TrainingOutcome runFullTrainingSync() {
        Map<String, Object> data = new HashMap<>();
        String message = null;

        data.put("mlClassifierEnabled", mlProperties.isEnabled());
        data.put("sparkTrainerPresent", sparkTrainer.isPresent());
        data.put("sparkClassifierPresent", sparkClassifier.isPresent());

        if (mlProperties.isEnabled() && sparkTrainer.isPresent()) {
            data.put("sparkEnabled", true);
            SparkIsolatedTrainResult preemptiveIsolated = null;
            boolean sparkTrainingFinished = false;

            if (mlProperties.isSparkTrainIsolatedFirst()) {
                preemptiveIsolated = tryRunSparkStandaloneWithAntlrIsolation();
                if (preemptiveIsolated != null && preemptiveIsolated.success) {
                    message = "Spark modeli izole JVM ile eğitildi (ANTLR çakışmasından kaçınıldı).";
                    data.put("sparkTrainedSamples", preemptiveIsolated.samples);
                    data.put("sparkTrainPath", "isolated-jvm-first");
                    if (preemptiveIsolated.accuracy != null) {
                        Map<String, Object> evaluation = new HashMap<>();
                        evaluation.put("accuracy", preemptiveIsolated.accuracy);
                        evaluation.put("weightedF1", preemptiveIsolated.weightedF1);
                        data.put("evaluation", evaluation);
                    }
                    scheduleSparkModelReload(data);
                    sparkTrainingFinished = true;
                } else if (preemptiveIsolated != null && preemptiveIsolated.error != null) {
                    data.put("sparkIsolatedFirstError", preemptiveIsolated.error);
                }
            }

            if (mlProperties.isSparkTrainIsolatedFirst() && preemptiveIsolated != null && !preemptiveIsolated.success) {
                sparkTrainingFinished = true;
                data.put("sparkTrainedSamples", 0);
                data.put("sparkInProcessSkippedAfterIsolatedFailure", true);
                if (message == null || message.isBlank()) {
                    message = "Spark izole eğitim başarısız; ana JVM Spark atlandı. Naive Bayes ile devam.";
                }
            }

            if (!sparkTrainingFinished) {
                try {
                    int samples = sparkTrainer.get().trainAndSave();
                    data.put("sparkTrainedSamples", samples);
                    MlEvaluationMetrics eval = sparkTrainer.get().getLastEvaluationMetrics();
                    if (eval != null) {
                        Map<String, Object> evaluation = new HashMap<>();
                        evaluation.put("accuracy", eval.getAccuracy());
                        evaluation.put("weightedPrecision", eval.getWeightedPrecision());
                        evaluation.put("weightedRecall", eval.getWeightedRecall());
                        evaluation.put("weightedF1", eval.getWeightedF1());
                        evaluation.put("testSampleCount", eval.getTestSampleCount());
                        data.put("evaluation", evaluation);
                    }
                    message = samples > 0
                            ? "Spark MLlib modeli eğitildi."
                            : "Spark MLlib modeli için yeterli veri bulunamadı.";
                } catch (Throwable t) {
                    log.error("Spark training failed", t);
                    boolean antlrConflictHandled = false;
                    data.put(
                            "sparkTrainingError",
                            t.getClass().getSimpleName() + ": " + String.valueOf(t.getMessage())
                    );

                    Throwable root = t;
                    int safety = 0;
                    while (root.getCause() != null && root.getCause() != root && safety++ < 20) {
                        root = root.getCause();
                    }

                    data.put(
                            "sparkTrainingRootCause",
                            root.getClass().getName() + ": " + String.valueOf(root.getMessage())
                    );

                    List<String> chain = new ArrayList<>();
                    Throwable cur = t;
                    int i = 0;
                    while (cur != null && i++ < 10) {
                        chain.add(cur.getClass().getSimpleName() + ": " + String.valueOf(cur.getMessage()));
                        cur = cur.getCause();
                    }
                    data.put("sparkTrainingErrorChain", chain);

                    if (isSparkAntlrClasspathConflict(t)) {
                        antlrConflictHandled = true;
                        data.put(
                                "sparkAntlrConflictHint",
                                "Spark (SqlBaseLexer) ANTLR 4.9.3 ile derlenmiş; classpath'te Hibernate nedeniyle " +
                                        "antlr4-runtime 4.13 öne çıkınca aynı JVM içinde çakışıyor. " +
                                        "İzole process yalnızca antlr4-runtime 4.9.3 ile çalışır."
                        );

                        if (mlProperties.isSparkTrainIsolatedFirst()) {
                            message = "Spark ANTLR çakışması: izole ön eğitim başarısız veya atlandı; ana JVM eğitimi de başarısız. Naive Bayes ile devam.";
                            data.put("sparkTrainedSamples", 0);
                            if (preemptiveIsolated != null && preemptiveIsolated.error != null) {
                                data.put("sparkAntlrIsolationError", preemptiveIsolated.error);
                            }
                        } else {
                            SparkIsolatedTrainResult isolated = tryRunSparkStandaloneWithAntlrIsolation();
                            if (isolated != null && isolated.success) {
                                message = "Spark ANTLR çakışması nedeniyle ana JVM'de çalışamadı; izole process ile eğitim yapıldı.";
                                data.put("sparkTrainedSamples", isolated.samples);
                                if (isolated.accuracy != null) {
                                    Map<String, Object> evaluation = new HashMap<>();
                                    evaluation.put("accuracy", isolated.accuracy);
                                    evaluation.put("weightedF1", isolated.weightedF1);
                                    data.put("evaluation", evaluation);
                                }
                                scheduleSparkModelReload(data);
                            } else {
                                message = "Spark ANTLR çakışması nedeniyle çalıştırılamadı; Naive Bayes ile devam ediliyor.";
                                data.put("sparkTrainedSamples", 0);
                                if (isolated != null && isolated.error != null) {
                                    data.put("sparkAntlrIsolationError", isolated.error);
                                }
                            }
                        }
                    }

                    if (!antlrConflictHandled) {
                        data.put("sparkTrainedSamples", 0);
                    }
                    String msg = t.getMessage() != null ? t.getMessage().toLowerCase(Locale.forLanguageTag("tr")) : "";
                    if (msg.contains("getsubject") && msg.contains("security manager")) {
                        data.put(
                                "sparkJvmArgsHint",
                                "Spark için JVM açılışında şu argümanlar gerekli: " +
                                        "`--add-opens=java.base/sun.nio.ch=ALL-UNNAMED -Djava.security.manager=allow`." +
                                        " IDE'den run ediyorsan Run/Debug VM options'a ekleyip uygulamayı yeniden başlat."
                        );
                    }
                    if (message == null || message.isBlank()) {
                        message = "Spark eğitimi hata verdi; Naive Bayes eğitimi ile devam ediliyor.";
                    }
                }
            }
        } else {
            data.put("sparkEnabled", false);
            message = "Spark devre dışı veya başlatılamadı. Naive Bayes eğitimi deneniyor.";
        }

        if (naiveBayesClassifier.isPresent()) {
            naiveBayesClassifier.get().train();
            data.put("naiveBayesReady", naiveBayesClassifier.get().isModelReady());
            MlEvaluationMetrics nbEval = naiveBayesClassifier.get().getLastEvaluationMetrics();
            if (nbEval != null) {
                Map<String, Object> nbEvaluation = new HashMap<>();
                nbEvaluation.put("accuracy", nbEval.getAccuracy());
                nbEvaluation.put("weightedPrecision", nbEval.getWeightedPrecision());
                nbEvaluation.put("weightedRecall", nbEval.getWeightedRecall());
                nbEvaluation.put("weightedF1", nbEval.getWeightedF1());
                nbEvaluation.put("macroF1", nbEval.getMacroF1());
                nbEvaluation.put("testSampleCount", nbEval.getTestSampleCount());
                data.put("naiveBayesEvaluation", nbEvaluation);
            }
        } else {
            data.put("naiveBayesReady", false);
        }

        return new TrainingOutcome(message != null ? message : "Tamamlandı.", data);
    }

    public void scheduleSparkModelReload(Map<String, Object> data) {
        if (sparkClassifier.isEmpty()) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                sparkClassifier.get().loadModel();
                log.info("Spark loadModel arka plan görevi tamamlandı.");
            } catch (Throwable ex) {
                log.error("Arka plan loadModel: {}", ex.getMessage());
            }
        });
        data.put("sparkModelReloadScheduled", true);
    }

    private static boolean isSparkAntlrClasspathConflict(Throwable t) {
        Throwable cur = t;
        for (int depth = 0; cur != null && depth < 25; depth++) {
            String msg = String.valueOf(cur.getMessage());
            String lower = msg.toLowerCase(Locale.ROOT);
            String name = cur.getClass().getName();

            if (lower.contains("could not deserialize atn")) {
                return true;
            }
            if (name.endsWith("InvalidClassException") && lower.contains("atn")) {
                return true;
            }
            if (lower.contains("antlr") && lower.contains("runtime version") && lower.contains("does not match")) {
                return true;
            }
            if (name.endsWith("UnsupportedOperationException") && lower.contains("invalidclassexception")
                    && lower.contains("atn")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static void drainProcessStream(InputStream in, StringBuilder out) {
        byte[] buf = new byte[16384];
        try (InputStream stream = in) {
            int n;
            while ((n = stream.read(buf)) != -1) {
                out.append(new String(buf, 0, n, StandardCharsets.UTF_8));
            }
        } catch (IOException ignored) {
        }
    }

    private static void joinQuietly(Thread t, long maxWaitMs) {
        try {
            t.join(maxWaitMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String findLastSparkTrainingResultLine(String full) {
        if (full == null || full.isBlank()) {
            return null;
        }
        String last = null;
        for (String line : full.split("\r?\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("SPARK_TRAINING_RESULT:")) {
                last = trimmed;
            }
        }
        return last;
    }

    /** İzole Spark child JVM: önce {@code ml.classifier.antlr493-jar-path}, yoksa ~/.m2 altındaki 4.9.3 jar. */
    private Path resolveAntlr493JarPathForIsolatedSpark() {
        String configured = mlProperties.getAntlr493JarPath();
        if (configured != null && !configured.isBlank()) {
            Path p = Path.of(configured.trim());
            if (Files.exists(p)) {
                return p.toAbsolutePath();
            }
        }
        Path m2 = Path.of(
                System.getProperty("user.home"),
                ".m2",
                "repository",
                "org",
                "antlr",
                "antlr4-runtime",
                "4.9.3",
                "antlr4-runtime-4.9.3.jar"
        );
        if (Files.exists(m2)) {
            return m2.toAbsolutePath();
        }
        return null;
    }

    private String describeAntlr493Lookup() {
        String configured = mlProperties.getAntlr493JarPath();
        Path m2 = Path.of(
                System.getProperty("user.home"),
                ".m2",
                "repository",
                "org",
                "antlr",
                "antlr4-runtime",
                "4.9.3",
                "antlr4-runtime-4.9.3.jar"
        );
        return "ml.classifier.antlr493-jar-path="
                + (configured != null && !configured.isBlank() ? configured : "(bos)")
                + "; m2.yolu=" + m2.toAbsolutePath();
    }

    private SparkIsolatedTrainResult tryRunSparkStandaloneWithAntlrIsolation() {
        try {
            String cp = System.getProperty("java.class.path");
            if (cp == null || cp.isBlank()) {
                return new SparkIsolatedTrainResult(false, 0, null, null, "java.class.path boş");
            }

            List<String> filtered = new ArrayList<>();
            for (String entry : cp.split(File.pathSeparator)) {
                String lower = entry.toLowerCase(Locale.ROOT);
                if (lower.contains("hibernate-core")) continue;
                if (lower.contains("antlr4-runtime-")) continue;
                filtered.add(entry);
            }

            Path antlr493 = resolveAntlr493JarPathForIsolatedSpark();
            if (antlr493 == null || !Files.exists(antlr493)) {
                return new SparkIsolatedTrainResult(false, 0, null, null,
                        "antlr4-runtime 4.9.3 jar bulunamadı. ml.classifier.antlr493-jar-path veya "
                                + "ML_CLASSIFIER_ANTLR493_JAR ile tam yol verin (Docker: /app/deps/antlr4-runtime-4.9.3.jar). "
                                + "Denenen: " + describeAntlr493Lookup());
            }
            filtered.add(antlr493.toAbsolutePath().toString());

            String javaBin = Path.of(System.getProperty("java.home"), "bin", "java").toAbsolutePath().toString();
            String childCp = String.join(File.pathSeparator, filtered);

            Path modelRoot = Path.of(mlProperties.getModelPath()).toAbsolutePath();

            List<String> cmd = new ArrayList<>();
            cmd.add(javaBin);
            cmd.add("--add-opens=java.base/sun.nio.ch=ALL-UNNAMED");
            cmd.add("-Djava.security.manager=allow");
            cmd.add("-cp");
            cmd.add(childCp);
            cmd.add("com.bitirme.nlp.SparkStandaloneTrainerMain");

            cmd.add("--dbUrl=" + datasourceUrl);
            cmd.add("--dbUser=" + datasourceUsername);
            cmd.add("--dbPassword=" + datasourcePassword);

            cmd.add("--modelPath=" + modelRoot);
            cmd.add("--sparkMaster=" + mlProperties.getSparkMaster());
            cmd.add("--ngramMin=" + mlProperties.getNGramMin());
            cmd.add("--ngramMax=" + mlProperties.getNGramMax());
            cmd.add("--numFeatures=" + mlProperties.getNumFeatures());
            cmd.add("--minTrainingSamples=" + mlProperties.getMinTrainingSamples());

            cmd.add("--runEvaluationAfterTrain=" + mlProperties.isRunEvaluationAfterTrain());
            cmd.add("--testSplitRatio=" + mlProperties.getTestSplitRatio());
            cmd.add("--evaluationSeed=" + mlProperties.getEvaluationSeed());

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new File("."));
            pb.redirectErrorStream(true);

            Process proc = pb.start();

            StringBuilder capturedOut = new StringBuilder(65536);
            Thread drain = new Thread(() -> drainProcessStream(proc.getInputStream(), capturedOut), "spark-isolated-stdout");
            drain.setDaemon(true);
            drain.start();

            long timeoutMs = Math.max(30_000L, mlProperties.getSparkIsolatedTrainTimeoutMs());
            boolean completed = proc.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!completed) {
                log.warn("İzole Spark eğitimi {} ms içinde bitmedi; süreç sonlandırılıyor.", timeoutMs);
                proc.destroyForcibly();
                joinQuietly(drain, 8000);
                return new SparkIsolatedTrainResult(false, 0, null, null,
                        "timeout: izole Spark eğitimi " + (timeoutMs / 1000) + " sn içinde bitmedi");
            }

            joinQuietly(drain, 15_000);

            String resultLine = findLastSparkTrainingResultLine(capturedOut.toString());
            Integer samples = 0;
            Double accuracy = null;
            Double weightedF1 = null;
            if (resultLine != null) {
                Map<String, String> kv = parseSparkResultLine(resultLine);
                if (kv.containsKey("samples")) samples = Integer.parseInt(kv.get("samples"));
                if (kv.containsKey("accuracy") && !"null".equalsIgnoreCase(kv.get("accuracy"))) {
                    accuracy = Double.parseDouble(kv.get("accuracy"));
                }
                if (kv.containsKey("weightedF1") && !"null".equalsIgnoreCase(kv.get("weightedF1"))) {
                    weightedF1 = Double.parseDouble(kv.get("weightedF1"));
                }
            }

            int exit = proc.exitValue();
            if (exit == 0 && samples != null && samples > 0) {
                return new SparkIsolatedTrainResult(true, samples, accuracy, weightedF1, null);
            }
            return new SparkIsolatedTrainResult(false, samples != null ? samples : 0, accuracy, weightedF1,
                    "process exit=" + exit);
        } catch (Throwable e) {
            return new SparkIsolatedTrainResult(false, 0, null, null, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private Map<String, String> parseSparkResultLine(String line) {
        String payload = line.substring("SPARK_TRAINING_RESULT:".length());
        String[] parts = payload.split(";");
        Map<String, String> kv = new HashMap<>();
        for (String part : parts) {
            if (!part.contains("=")) continue;
            String[] xy = part.split("=", 2);
            kv.put(xy[0], xy[1]);
        }
        return kv;
    }

    private static class SparkIsolatedTrainResult {
        final boolean success;
        final Integer samples;
        final Double accuracy;
        final Double weightedF1;
        final String error;

        private SparkIsolatedTrainResult(boolean success, Integer samples, Double accuracy, Double weightedF1, String error) {
            this.success = success;
            this.samples = samples;
            this.accuracy = accuracy;
            this.weightedF1 = weightedF1;
            this.error = error;
        }
    }

    public record TrainingOutcome(String message, Map<String, Object> data) {}

    public record TrainingSnapshot(Instant finishedAt, String message, Map<String, Object> data, Throwable failure) {}
}
