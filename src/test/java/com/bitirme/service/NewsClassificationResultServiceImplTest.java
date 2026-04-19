package com.bitirme.service;

import com.bitirme.dto.news.NewsClassificationResultCreateRequest;
import com.bitirme.entity.Category;
import com.bitirme.entity.ModelVersion;
import com.bitirme.entity.News;
import com.bitirme.entity.NewsClassificationResult;
import com.bitirme.exception.NotFoundException;
import com.bitirme.repository.CategoryRepository;
import com.bitirme.repository.ModelVersionRepository;
import com.bitirme.repository.NewsClassificationResultRepository;
import com.bitirme.repository.NewsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsClassificationResultServiceImplTest {

    @Mock
    private NewsClassificationResultRepository newsClassificationResultRepository;
    @Mock
    private NewsRepository newsRepository;
    @Mock
    private ModelVersionRepository modelVersionRepository;
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private NewsClassificationResultServiceImpl service;

    @Test
    @DisplayName("create haber yoksa NotFoundException")
    void create_news_missing() {
        NewsClassificationResultCreateRequest req = new NewsClassificationResultCreateRequest();
        req.setNewsId(1L);
        req.setModelVersionId(1);
        req.setPredictedCategoryId(1);
        when(newsRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("create tüm referanslar varsa kayıt yapılır")
    void create_success() {
        NewsClassificationResultCreateRequest req = new NewsClassificationResultCreateRequest();
        req.setNewsId(1L);
        req.setModelVersionId(1);
        req.setPredictedCategoryId(2);
        req.setPredictionScore(BigDecimal.valueOf(0.9));

        News news = new News();
        news.setId(1L);
        ModelVersion mv = new ModelVersion();
        mv.setId(1);
        Category cat = new Category();
        cat.setId(2);

        when(newsRepository.findById(1L)).thenReturn(Optional.of(news));
        when(modelVersionRepository.findById(1)).thenReturn(Optional.of(mv));
        when(categoryRepository.findById(2)).thenReturn(Optional.of(cat));
        when(newsClassificationResultRepository.save(any(NewsClassificationResult.class))).thenAnswer(inv -> {
            NewsClassificationResult r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        service.create(req);

        verify(newsClassificationResultRepository).save(any(NewsClassificationResult.class));
    }
}
