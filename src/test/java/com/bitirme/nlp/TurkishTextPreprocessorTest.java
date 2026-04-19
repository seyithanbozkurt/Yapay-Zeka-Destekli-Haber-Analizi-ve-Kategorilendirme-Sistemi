package com.bitirme.nlp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TurkishTextPreprocessorTest {

    private final TurkishTextPreprocessor preprocessor = new TurkishTextPreprocessor();

    @Test
    @DisplayName("null veya boş metin boş string döner")
    void preprocessNullOrBlankTest() {
        assertThat(preprocessor.preprocess((String) null)).isEmpty();
        assertThat(preprocessor.preprocess("   ")).isEmpty();
        assertThat(preprocessor.preprocess(null, null)).isEmpty();
    }

    @Test
    @DisplayName("başlık ve içerik birleştirilip tokenize edilir")
    void preprocessTitleAndContentTest() {
        String out = preprocessor.preprocess(
                "Galatasaray ve Fenerbahçe Maçı",
                "Bu hafta sonu için büyük derbi."
        );
        assertThat(out).contains("galatasaray");
        assertThat(out).contains("fenerbahçe");
        assertThat(out).contains("maçı");
    }

    @Test
    @DisplayName("preprocessToTokens boş olmayan token listesi döner")
    void preprocessToTokensSplitsOnWhitespaceTest() {
        List<String> tokens = preprocessor.preprocessToTokens("Ekonomi borsa dolar");
        assertThat(tokens).isNotEmpty();
        assertThat(String.join(" ", tokens)).contains("ekonomi");
    }
}
