package com.bitirme.nlp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ML sınıflandırıcı ayarları: Spark model yolu, özellik çıkarımı (n-gram, TF-IDF),
 * öğrenme parametreleri ve model değerlendirme.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ml.classifier")
public class MlClassifierProperties {

    // ML sınıflandırıcı kullanılsın mı (false ise keyword-based fallback).
    private boolean enabled = false;

    /// Spark master (local mode için local[*]).
    private String sparkMaster = "local[*]";

    // Eğitilmiş PipelineModel'in kaydedildiği dizin.
    private String modelPath = "data/ml-model";

    // Minimum güven eşiği (altındaki tahminler 'Diğer' sayılabilir).
    private double minConfidence = 0.0;

    // Eğitim için minimum etiketli haber sayısı.
    private int minTrainingSamples = 50;

    // --- Özellik çıkarımı (n-gram) ---

    // N-gram minimum n (1 = unigram dahil).
    private int nGramMin = 1;
    // N-gram maximum n (2 = bigram, 3 = trigram).
    private int nGramMax = 3;

    // HashingTF / CountVectorizer için özellik sayısı (2^18 = 262144).
    private int numFeatures = 262144;

    // --- Linear SVM (öğrenme katsayıları) ---

    // Regularizasyon parametresi (küçük = daha güçlü regularizasyon).
    private double regParam = 0.05;
    // Maksimum iterasyon sayısı.
    private int maxIter = 150;

    // --- Model değerlendirme ---

    // Eğitim sonrası test seti üzerinde değerlendirme yapılsın mı (train/test split).
    private boolean runEvaluationAfterTrain = true;
    // Test seti oranı (0.2 = %20 test, %80 eğitim).
    private double testSplitRatio = 0.2;
    // Değerlendirme için rastgele seed (tekrarlanabilirlik).
    private long evaluationSeed = 42L;
}
