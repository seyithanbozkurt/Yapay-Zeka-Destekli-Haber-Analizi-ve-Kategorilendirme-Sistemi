package com.bitirme.controller;

import com.bitirme.dto.common.ApiResponse;
import com.bitirme.nlp.MlEvaluationMetrics;
import com.bitirme.nlp.NaiveBayesNewsClassifier;
import com.bitirme.nlp.SparkNewsClassifier;
import com.bitirme.nlp.SparkNewsClassifierTrainer;
import com.bitirme.nlp.config.MlClassifierProperties;
import com.bitirme.service.MlModelTrainingService;
import com.bitirme.service.MlTrainingDataSeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
@Tag(name = "ML Sınıflandırıcı", description = "Spark MLlib ve Naive Bayes modellerini yönetme")
@Slf4j
public class MlClassifierController {

    private final MlClassifierProperties mlProperties;
    private final Optional<SparkNewsClassifierTrainer> sparkTrainer;
    private final Optional<SparkNewsClassifier> sparkClassifier;
    private final Optional<NaiveBayesNewsClassifier> naiveBayesClassifier;
    private final MlModelTrainingService mlModelTrainingService;
    private final MlTrainingDataSeedService mlTrainingDataSeedService;

    @PostMapping("/seed-and-train")
    @Tag(name = "YZ-AI: Tohumlama ve eğitim", description = "Dengeli eğitim verisi üretimi ve model eğitimi (Swagger’da arayın: seed veya mlSeedAndTrain)")
    @Operation(
            operationId = "mlSeedAndTrain",
            summary = "Dengeli yapay zeka eğitim verisi + tam eğitim",
            description = "Her aktif kategoride yeterli aktif etiket yoksa sentetik haber ekler (ml.classifier.min-training-samples, app.ml.seed.min-per-category), "
                    + "ardından POST /api/ml/train ile aynı tam eğitimi senkron çalıştırır ve Spark modelini diske yükler. İstek gövdesi gerekmez."
    )
    @RequestBody(required = false, content = @Content())
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Tohumlama ve eğitim tamam; yanıt gövdesinde trainingData, sparkModelLoaded vb."
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Hata; data içinde 'error' anahtarı (ör. kategori veya model_versions yok)"
            )
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> seedAndTrain() {
        Map<String, Object> data = mlTrainingDataSeedService.seedBalancedLabelsAndTrainSync();
        if (data == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Servis yanıtı null.");
            return badRequestWithError(err);
        }
        if (data.containsKey("error")) {
            return badRequestWithError(data);
        }
        return ResponseEntity.ok(ApiResponse.success(
                "Dengeli eğitim verisi hazırlandı ve model eğitimi tamamlandı.",
                data
        ));
    }

    private static ResponseEntity<ApiResponse<Map<String, Object>>> badRequestWithError(Map<String, Object> data) {
        Object msg = data.get("error");
        return ResponseEntity.badRequest().body(
                ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message(msg != null ? String.valueOf(msg) : "İşlem başarısız")
                        .data(data)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @PostMapping("/train")
    @Operation(
            summary = "ML model(ler)ini eğit",
            description = "ml.classifier.train-async=true iken hemen yanıt döner; eğitim arka planda. Sonuç: GET /api/ml/train/last"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> train() {
        if (mlProperties.isTrainAsync()) {
            boolean queued = mlModelTrainingService.tryRunTrainingAsync(null);
            if (!queued) {
                Map<String, Object> data = new HashMap<>();
                data.put("trainingInProgress", true);
                data.put("hint", "Önceki eğitim bitene kadar bekleyin; GET /api/ml/train/last ile durumu kontrol edin.");
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.success("Eğitim zaten çalışıyor.", data));
            }
            Map<String, Object> data = new HashMap<>();
            data.put("trainAsync", true);
            data.put("trainingQueued", true);
            data.put("hint", "Eğitim arka planda. Birkaç saniye–dakika sonra GET /api/ml/train/last veya log çıktısı.");
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.success("Eğitim arka planda başlatıldı.", data));
        }

        MlModelTrainingService.TrainingOutcome outcome = mlModelTrainingService.runFullTrainingSync();
        return ResponseEntity.ok(ApiResponse.success(outcome.message(), outcome.data()));
    }

    @GetMapping("/train/last")
    @Operation(summary = "Son tamamlanan ML eğitim sonucu", description = "train-async ile başlatılan eğitimin çıktısı (bitince dolu olur).")
    public ResponseEntity<ApiResponse<Map<String, Object>>> trainLast() {
        MlModelTrainingService.TrainingSnapshot snap = mlModelTrainingService.getLastTrainingSnapshot();
        Map<String, Object> data = new HashMap<>();
        data.put("trainingInProgress", mlModelTrainingService.isTrainingInProgress());
        if (snap == null) {
            data.put("message", "Henüz tamamlanan eğitim yok.");
            return ResponseEntity.ok(ApiResponse.success(null, data));
        }
        data.put("finishedAt", snap.finishedAt().toString());
        if (snap.failure() != null) {
            data.put("failure", snap.failure().getClass().getSimpleName() + ": " + snap.failure().getMessage());
        }
        if (snap.message() != null) {
            data.put("message", snap.message());
        }
        if (snap.data() != null) {
            data.putAll(snap.data());
        }
        return ResponseEntity.ok(ApiResponse.success("Son eğitim özeti.", data));
    }

    @PostMapping("/reload")
    @Operation(
            summary = "Model(ler)i yeniden yükle",
            description = "Spark modeli diskten yeniden yükler; Naive Bayes modelini yeniden eğitir."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> reload() {
        Map<String, Object> data = new HashMap<>();

        if (mlProperties.isEnabled() && sparkClassifier.isPresent()) {
            mlModelTrainingService.scheduleSparkModelReload(data);
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
    @Operation(summary = "ML model durumu", description = "Spark ve Naive Bayes modellerinin durumunu döner.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status() {
        Map<String, Object> data = new HashMap<>();

        boolean sparkLoaded = mlProperties.isEnabled()
                && sparkClassifier.isPresent()
                && sparkClassifier.get().isModelLoaded();
        data.put("sparkEnabled", mlProperties.isEnabled());
        data.put("sparkModelLoaded", sparkLoaded);

        boolean nbReady = naiveBayesClassifier.isPresent() && naiveBayesClassifier.get().isModelReady();
        data.put("naiveBayesReady", nbReady);
        data.put("mlTrainAsync", mlProperties.isTrainAsync());
        data.put("trainingInProgress", mlModelTrainingService.isTrainingInProgress());

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
        if (eval != null) {
            data.put("source", "spark");
            data.put("accuracy", eval.getAccuracy());
            data.put("weightedPrecision", eval.getWeightedPrecision());
            data.put("weightedRecall", eval.getWeightedRecall());
            data.put("weightedF1", eval.getWeightedF1());
            data.put("macroF1", eval.getMacroF1());
            data.put("testSampleCount", eval.getTestSampleCount());
            return ResponseEntity.ok(ApiResponse.success("Son eğitim değerlendirme metrikleri.", data));
        }
        if (naiveBayesClassifier.isPresent()) {
            MlEvaluationMetrics nbEval = naiveBayesClassifier.get().getLastEvaluationMetrics();
            if (nbEval != null) {
                data.put("source", "naive-bayes");
                data.put("accuracy", nbEval.getAccuracy());
                data.put("weightedPrecision", nbEval.getWeightedPrecision());
                data.put("weightedRecall", nbEval.getWeightedRecall());
                data.put("weightedF1", nbEval.getWeightedF1());
                data.put("macroF1", nbEval.getMacroF1());
                data.put("testSampleCount", nbEval.getTestSampleCount());
                return ResponseEntity.ok(ApiResponse.success("Naive Bayes değerlendirme metrikleri.", data));
            }
        }
        data.put("message", "Henüz değerlendirme yapılmadı. POST /api/ml/train ile eğitim yapın (ml.classifier.run-evaluation-after-train=true).");
        return ResponseEntity.ok(ApiResponse.success(null, data));
    }
}
