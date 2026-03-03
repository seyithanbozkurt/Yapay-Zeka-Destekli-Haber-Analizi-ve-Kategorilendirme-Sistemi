package com.bitirme.controller;

import com.bitirme.dto.common.ApiResponse;
import com.bitirme.nlp.SparkNewsClassifier;
import com.bitirme.nlp.SparkNewsClassifierTrainer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
@Tag(name = "ML Sınıflandırıcı", description = "Spark ML model eğitimi ve yeniden yükleme")
public class MlClassifierController {

    private final Optional<SparkNewsClassifierTrainer> trainer;
    private final Optional<SparkNewsClassifier> classifier;

    @PostMapping("/train")
    @Operation(summary = "ML modelini eğit", description = "Veritabanındaki etiketli haberlerle TF-IDF + Naive Bayes modelini eğitir ve kaydeder.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> train() {
        if (trainer.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error("ML sınıflandırıcı etkin değil veya Spark başlatılamadı (ml.classifier.enabled=false)."));
        }
        int samples = trainer.get().trainAndSave();
        return ResponseEntity.ok(ApiResponse.success(
                "Eğitim tamamlandı",
                Map.of("trainedSamples", samples)
        ));
    }

    @PostMapping("/reload")
    @Operation(summary = "Modeli yeniden yükle", description = "Diske kaydedilmiş modeli yeniden yükler.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reload() {
        if (classifier.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error("ML sınıflandırıcı etkin değil veya Spark başlatılamadı (ml.classifier.enabled=false)."));
        }
        classifier.get().loadModel();
        return ResponseEntity.ok(ApiResponse.success(
                "Model yeniden yüklendi",
                Map.of("modelLoaded", classifier.get().isModelLoaded())
        ));
    }

    @GetMapping("/status")
    @Operation(summary = "ML model durumu", description = "Modelin yüklü olup olmadığını döner.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status() {
        boolean loaded = classifier.isPresent() && classifier.get().isModelLoaded();
        return ResponseEntity.ok(ApiResponse.success(
                null,
                Map.of("modelLoaded", loaded)
        ));
    }
}
