package com.bitirme.nlp;

import com.bitirme.entity.News;
import com.bitirme.nlp.config.MlClassifierProperties;
import com.bitirme.repository.NewsClassificationResultRepository;
import com.bitirme.repository.NewsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NaiveBayesNewsClassifierTest {

    @Mock
    private NewsClassificationResultRepository classificationResultRepository;
    @Mock
    private NewsRepository newsRepository;
    @Mock
    private TurkishTextPreprocessor preprocessor;
    @Mock
    private MlClassifierProperties properties;

    private NaiveBayesNewsClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new NaiveBayesNewsClassifier(
                classificationResultRepository,
                newsRepository,
                preprocessor,
                properties
        );
    }

    @Test
    @DisplayName("aktif sonuç yokken train modeli hazır etmez")
    void train_empty_results_model_not_ready() {
        when(classificationResultRepository.findByActiveTrue()).thenReturn(Collections.emptyList());

        classifier.train();

        assertThat(classifier.isModelReady()).isFalse();
        assertThat(classifier.getLastEvaluationMetrics()).isNull();
        verify(classificationResultRepository).findByActiveTrue();
    }

    @Test
    @DisplayName("model hazır değilken classify boş döner")
    void classify_when_not_ready_returns_empty() {
        when(classificationResultRepository.findByActiveTrue()).thenReturn(Collections.emptyList());
        classifier.train();

        News news = new News();
        news.setId(1L);
        news.setTitle("Herhangi bir başlık metni");
        news.setContent("İçerik metni yeterince uzun olsun.");

        assertThat(classifier.classify(news)).isEmpty();
    }

    @Test
    @DisplayName("null haber için classify boş")
    void classify_null_news_empty() {
        assertThat(classifier.classify(null)).isEmpty();
    }
}
