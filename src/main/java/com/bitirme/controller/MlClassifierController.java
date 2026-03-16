package com.bitirme.controller;

import com.bitirme.dto.common.ApiResponse;
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

        // 1) Tercih: Spark Lineer SVM eğitimi
        if (mlProperties.isEnabled() && sparkTrainer.isPresent()) {
            int samples = sparkTrainer.get().trainAndSave();
            data.put("sparkEnabled", true);
            data.put("sparkTrainedSamples", samples);
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
}
