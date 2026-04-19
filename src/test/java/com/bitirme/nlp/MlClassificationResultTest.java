package com.bitirme.nlp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MlClassificationResultTest {

    @Test
    @DisplayName("of fabrika metodu kategori ve güveni set eder")
    void ofSetsCategoryAndConfidenceTest() {
        MlClassificationResult r = MlClassificationResult.of("Spor", 0.85);
        assertThat(r.getCategoryName()).isEqualTo("Spor");
        assertThat(r.getConfidence()).isEqualByComparingTo(BigDecimal.valueOf(0.85));
    }
}
