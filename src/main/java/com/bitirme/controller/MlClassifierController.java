package com.bitirme.controller;

import com.bitirme.dto.common.ApiResponse;
import com.bitirme.nlp.MlEvaluationMetrics;
import com.bitirme.nlp.NaiveBayesNewsClassifier;
import com.bitirme.nlp.SparkNewsClassifier;
import com.bitirme.nlp.SparkNewsClassifierTrainer;
import com.bitirme.nlp.config.MlClassifierProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
@Tag(name = "ML Sınıflandırıcı", description = "Spark (Lineer SVM) ve Naive Bayes modellerini yönetme")
public class MlClassifierController {

    private final MlClassifierProperties mlProperties;
    private final Optional<SparkNewsClassifierTrainer> sparkTrainer;
    private final Optional<SparkNewsClassifier> sparkClassifier;
    private final Optional<NaiveBayesNewsClassifier> naiveBayesClassifier;

    @PostMapping("/train")
    @Operation(
            summary = "ML model(ler)ini eğit",
            description = "Önce Spark Lineer SVM (varsa) modeli eğitilir; başarısız veya devre dışıysa Naive Bayes modeli eğitilir."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> train() {
        Map<String, Object> data = new HashMap<>();
        String message;

        // 1) Tercih: Spark Lineer SVM eğitimi (N-gram + TF-IDF + Linear SVM)
        if (mlProperties.isEnabled() && sparkTrainer.isPresent()) {
            int samples = sparkTrainer.get().trainAndSave();
            data.put("sparkEnabled", true);
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
                    ? "Spark Lineer SVM modeli eğitildi."
                    : "Spark Lineer SVM modeli için yeterli veri bulunamadı.";
        } else {
            data.put("sparkEnabled", false);
            message = "Spark devre dışı veya başlatılamadı. Naive Bayes eğitimi deneniyor.";
        }

        // 2) Naive Bayes eğitimi (her durumda deneyebiliriz)
        if (naiveBayesClassifier.isPresent()) {
            naiveBayesClassifier.get().train();
            data.put("naiveBayesReady", naiveBayesClassifier.get().isModelReady());
        } else {
            data.put("naiveBayesReady", false);
        }

        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

    @PostMapping("/reload")
    @Operation(
            summary = "Model(ler)i yeniden yükle",
            description = "Spark modeli diskten yeniden yükler; Naive Bayes modelini yeniden eğitir."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> reload() {
        Map<String, Object> data = new HashMap<>();

        if (mlProperties.isEnabled() && sparkClassifier.isPresent()) {
            sparkClassifier.get().loadModel();
            data.put("sparkModelLoaded", sparkClassifier.get().isModelLoaded());
        } else {
            data.put("sparkModelLoaded", false);
        }

        if (naiveBayesClassifier.isPresent()) {
            naiveBayesClassifier.get().train();
            data.put("naiveBayesReady", naiveBayesClassifier.get().isModelReady());
        } else {
            data.put("naiveBayesReady", false);
        }

        return ResponseEntity.ok(ApiResponse.success("Modeller yeniden yüklendi/yeniden eğitildi.", data));
    }

    @GetMapping("/status")
    @Operation(summary = "ML model durumu", description = "Spark Lineer SVM ve Naive Bayes modellerinin durumunu döner.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status() {
        Map<String, Object> data = new HashMap<>();

        boolean sparkLoaded = mlProperties.isEnabled()
                && sparkClassifier.isPresent()
                && sparkClassifier.get().isModelLoaded();
        data.put("sparkEnabled", mlProperties.isEnabled());
        data.put("sparkModelLoaded", sparkLoaded);

        boolean nbReady = naiveBayesClassifier.isPresent() && naiveBayesClassifier.get().isModelReady();
        data.put("naiveBayesReady", nbReady);

        return ResponseEntity.ok(ApiResponse.success(null, data));
    }

    @GetMapping("/evaluate")
    @Operation(
            summary = "Son değerlendirme metrikleri",
            description = "Son eğitimde (train/test split ile) hesaplanan Accuracy, Precision, Recall, F1 değerlerini döner."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> evaluate() {
        Map<String, Object> data = new HashMap<>();
        if (sparkTrainer.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error("Spark ML devre dışı. Değerlendirme sadece Spark eğitimi sonrası kullanılabilir."));
        }
        MlEvaluationMetrics eval = sparkTrainer.get().getLastEvaluationMetrics();
        if (eval == null) {
            data.put("message", "Henüz değerlendirme yapılmadı. POST /api/ml/train ile eğitim yapın (ml.classifier.run-evaluation-after-train=true).");
            return ResponseEntity.ok(ApiResponse.success(null, data));
        }
        data.put("accuracy", eval.getAccuracy());
        data.put("weightedPrecision", eval.getWeightedPrecision());
        data.put("weightedRecall", eval.getWeightedRecall());
        data.put("weightedF1", eval.getWeightedF1());
        data.put("macroF1", eval.getMacroF1());
        data.put("testSampleCount", eval.getTestSampleCount());
        return ResponseEntity.ok(ApiResponse.success("Son eğitim değerlendirme metrikleri.", data));
    }
}
