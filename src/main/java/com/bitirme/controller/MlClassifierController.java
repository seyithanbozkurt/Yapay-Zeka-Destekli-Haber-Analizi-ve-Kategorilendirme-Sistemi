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

@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
@Tag(name = "ML Sınıflandırıcı", description = "Spark ML model eğitimi ve yeniden yükleme")
public class MlClassifierController {

    private final SparkNewsClassifierTrainer trainer;
    private final SparkNewsClassifier classifier;

    @PostMapping("/train")
    @Operation(summary = "ML modelini eğit", description = "Veritabanındaki etiketli haberlerle TF-IDF + Naive Bayes modelini eğitir ve kaydeder.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> train() {
        int samples = trainer.trainAndSave();
        return ResponseEntity.ok(ApiResponse.success(
                "Eğitim tamamlandı",
                Map.of("trainedSamples", samples)
        ));
    }

    @PostMapping("/reload")
    @Operation(summary = "Modeli yeniden yükle", description = "Diske kaydedilmiş modeli yeniden yükler.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reload() {
        classifier.loadModel();
        return ResponseEntity.ok(ApiResponse.success(
                "Model yeniden yüklendi",
                Map.of("modelLoaded", classifier.isModelLoaded())
        ));
    }

    @GetMapping("/status")
    @Operation(summary = "ML model durumu", description = "Modelin yüklü olup olmadığını döner.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status() {
        return ResponseEntity.ok(ApiResponse.success(
                null,
                Map.of("modelLoaded", classifier.isModelLoaded())
        ));
    }
}
