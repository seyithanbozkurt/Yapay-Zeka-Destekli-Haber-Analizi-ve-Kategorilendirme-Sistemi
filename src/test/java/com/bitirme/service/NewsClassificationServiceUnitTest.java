package com.bitirme.service;

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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
    void predictAsayisFromTitleTest() {
        String cat = newsClassificationService.predictCategoryKeywordOnly(
                "Cinayet zanlısı adliyede",
                "Mahkeme süreci devam ediyor."
        );
        assertThat(cat).isEqualTo("Asayiş");
    }

    @Test
    @DisplayName("predictCategoryKeywordOnly spor başlığında Spor")
    void predictSporTest() {
        String cat = newsClassificationService.predictCategoryKeywordOnly(
                "Fenerbahçe maçı ne zaman",
                "Detaylar"
        );
        assertThat(cat).isEqualTo("Spor");
    }
    @Test
    @DisplayName("beyaz kutu: Turizm kritik başlık sinyalleri fallback'e gitmeden Turizm döner")
    void predictTurizmTest() {
        String cat = newsClassificationService.predictCategoryKeywordOnly(
                "Otel rezervasyon sayıları arttı",
                "Borsa, dolar ve faiz konuşulsa da turizm haberi."
        );

        assertThat(cat).isEqualTo("Turizm");
        verifyNoInteractions(categoryRepository);
    }

    @Test
    @DisplayName("anahtar kelime yokken DB'de Diğer yoksa ve kategori listesi boşsa null")
    void predictGibberishNoCategoryMatchReturnsNullTest() {
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
    void predictMatchesCategoryNameFromDatabaseTest() {
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
