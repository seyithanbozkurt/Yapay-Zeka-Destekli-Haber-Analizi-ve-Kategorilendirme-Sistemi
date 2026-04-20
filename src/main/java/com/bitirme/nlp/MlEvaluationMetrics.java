package com.bitirme.nlp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Model değerlendirme metrikleri: Accuracy, Precision, Recall, F1 (macro/weighted).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MlEvaluationMetrics {

    // Doğruluk (Accuracy): doğru tahmin / toplam.
    private double accuracy;

    // Ağırlıklı Precision (sınıf bazlı ortalaması).
    private double weightedPrecision;

    // Ağırlıklı Recall (sınıf bazlı ortalaması).
    private double weightedRecall;

    // Ağırlıklı F1 Score.
    private double weightedF1;

    // Macro F1 (sınıflar eşit ağırlıklı).
    private double macroF1;

    // Test seti örnek sayısı.
    private int testSampleCount;

    // Sınıf bazlı precision (kategori adı -> değer).
    private Map<String, Double> precisionByClass;

    // Sınıf bazlı recall (kategori adı -> değer).
    private Map<String, Double> recallByClass;

    // Sınıf bazlı F1 (kategori adı -> değer).
    private Map<String, Double> f1ByClass;
}
