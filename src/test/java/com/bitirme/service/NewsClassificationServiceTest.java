package com.bitirme.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kategorilendirme doğruluğu testleri.
 * Anahtar kelime + başlık ağırlığı ile doğru kategorinin atandığını doğrular.
 */
@SpringBootTest
class NewsClassificationServiceTest {

    @Autowired(required = false)
    private NewsClassificationService newsClassificationService;

    @Test
    @DisplayName("Cinayet/ceza haberi Asayiş kategorisine düşmeli")
    void asayisCinayetCezaTest() {
        if (newsClassificationService == null) return;
        String title = "Oğlunu parkta 37 bıçak darbesi ile öldürmüştü: Cezası belli oldu";
        String content = "Mahkeme kararı açıklandı.";
        String category = newsClassificationService.predictCategoryKeywordOnly(title, content);
        assertThat(category).isEqualTo("Asayiş");
    }

    @Test
    @DisplayName("Bıçak ve ceza geçen haber Asayiş olmalı (içerikte başka kelimeler olsa bile)")
    void asayisBaskinBaslikTest() {
        if (newsClassificationService == null) return;
        String title = "Bıçaklı saldırı: Sanık ceza aldı";
        String content = "Yol ve trafik düzenlemesi kapsamında bölgede çalışma yapıldı. Mahkeme kararı açıklandı.";
        String category = newsClassificationService.predictCategoryKeywordOnly(title, content);
        assertThat(category).isEqualTo("Asayiş");
    }

    @Test
    @DisplayName("Savaş kelimesi geçen haber Savaş kategorisinde olmalı")
    void savasKategoriTest() {
        if (newsClassificationService == null) return;
        String title = "Savaş bölgesinde ateşkes ilan edildi";
        String content = "Çatışma tarafları anlaştı.";
        String category = newsClassificationService.predictCategoryKeywordOnly(title, content);
        assertThat(category).isEqualTo("Savaş");
    }

    @Test
    @DisplayName("Trafik kazası haberi Ulaşım olmalı")
    void ulasimTrafikKazasiTest() {
        if (newsClassificationService == null) return;
        String title = "Ankara'da trafik kazası: 2 yaralı";
        String content = "Trafik yoğunluğu nedeniyle kaza meydana geldi.";
        String category = newsClassificationService.predictCategoryKeywordOnly(title, content);
        assertThat(category).isEqualTo("Ulaşım");
    }

    @Test
    @DisplayName("Sadece 'kaza' geçen haber Ulaşım değil Asayiş/Diğer olabilir (trafik kazası değilse)")
    void kazaTekBasinaUlasimDegilTest() {
        if (newsClassificationService == null) return;
        String title = "İş kazası: 1 işçi yaralandı";
        String content = "Fabrikada kaza meydana geldi.";
        String category = newsClassificationService.predictCategoryKeywordOnly(title, content);
        assertThat(category).isNotEqualTo("Ulaşım");
    }

    @Test
    @DisplayName("Spor haberi Spor kategorisinde olmalı")
    void sporKategoriTest() {
        if (newsClassificationService == null) return;
        String title = "Süper Lig'de maç sonucu: Galatasaray kazandı";
        String content = "Futbol maçı heyecanlı geçti.";
        String category = newsClassificationService.predictCategoryKeywordOnly(title, content);
        assertThat(category).isEqualTo("Spor");
    }

    @Test
    @DisplayName("Ekonomi haberi Ekonomi kategorisinde olmalı")
    void ekonomiKategoriTest() {
        if (newsClassificationService == null) return;
        String title = "Merkez Bankası faiz kararı açıkladı";
        String content = "Dolar ve euro kuru yükselişte.";
        String category = newsClassificationService.predictCategoryKeywordOnly(title, content);
        assertThat(category).isEqualTo("Ekonomi");
    }
}
