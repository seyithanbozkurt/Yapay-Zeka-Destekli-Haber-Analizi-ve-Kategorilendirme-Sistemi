package com.bitirme.service;

import com.bitirme.nlp.NaiveBayesNewsClassifier;
import com.bitirme.nlp.SparkNewsClassifier;
import com.bitirme.nlp.config.MlClassifierProperties;
import com.bitirme.repository.CategoryRepository;
import com.bitirme.repository.ModelVersionRepository;
import com.bitirme.repository.NewsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bitirme.entity.Category;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * {@link NewsClassificationService} için Mockito tabanlı hızlı birim testleri
 * (mevcut {@link NewsClassificationServiceTest} SpringBootTest ile tamamlayıcıdır).
 */
@ExtendWith(MockitoExtension.class)
class NewsClassificationServiceUnitTest {

    @Mock
    private NewsRepository newsRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ModelVersionRepository modelVersionRepository;
    @Mock
    private NewsClassificationResultService classificationResultService;
    @Mock
    private MlClassifierProperties mlClassifierProperties;

    private NewsClassificationService newsClassificationService;

    @BeforeEach
    void setUp() {
        newsClassificationService = new NewsClassificationService(
                newsRepository,
                categoryRepository,
                modelVersionRepository,
                classificationResultService,
                mlClassifierProperties,
                Optional.empty(),
                Optional.empty()
        );
    }

    @Test
    @DisplayName("predictCategoryKeywordOnly cinayet başlığında Asayiş")
    void predict_asayis_from_title() {
        String cat = newsClassificationService.predictCategoryKeywordOnly(
                "Cinayet zanlısı adliyede",
                "Mahkeme süreci devam ediyor."
        );
        assertThat(cat).isEqualTo("Asayiş");
    }

    @Test
    @DisplayName("predictCategoryKeywordOnly spor başlığında Spor")
    void predict_spor() {
        String cat = newsClassificationService.predictCategoryKeywordOnly(
                "Galatasaray maçı ne zaman",
                "Detaylar"
        );
        assertThat(cat).isEqualTo("Spor");
    }

    @Test
    @DisplayName("anahtar kelime yokken DB'de Diğer yoksa ve kategori listesi boşsa null")
    void predict_gibberish_no_category_match_returns_null() {
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());
        when(categoryRepository.findByName("Diğer")).thenReturn(Optional.empty());

        String cat = newsClassificationService.predictCategoryKeywordOnly(
                "xyzqwertyunique999",
                "asdfghjklmnp"
        );
        assertThat(cat).isNull();
    }

    @Test
    @DisplayName("anahtar kelime yokken metinde geçen kategori adı ile eşleşir")
    void predict_matches_category_name_from_database() {
        Category catRow = new Category();
        catRow.setName("Qwertyuniquecatname");
        catRow.setActive(true);
        when(categoryRepository.findAll()).thenReturn(List.of(catRow));

        String cat = newsClassificationService.predictCategoryKeywordOnly(
                "Özet",
                "Ayrıntılar qwertyuniquecatname başlığında."
        );
        assertThat(cat).isEqualTo("Qwertyuniquecatname");
    }
}
