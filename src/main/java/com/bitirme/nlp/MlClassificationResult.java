package com.bitirme.nlp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * ML modelinin tek bir haber için döndürdüğü tahmin sonucu.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MlClassificationResult {

    private String categoryName;
    private BigDecimal confidence;

    public static MlClassificationResult of(String categoryName, double confidence) {
        return new MlClassificationResult(categoryName, BigDecimal.valueOf(confidence));
    }
}
