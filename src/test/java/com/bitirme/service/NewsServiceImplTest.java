package com.bitirme.service;

import com.bitirme.dto.news.NewsCreateRequest;
import com.bitirme.dto.news.NewsResponse;
import com.bitirme.entity.News;
import com.bitirme.entity.Source;
import com.bitirme.exception.AlreadyExistsException;
import com.bitirme.exception.NotFoundException;
import com.bitirme.repository.CategoryRepository;
import com.bitirme.repository.NewsRepository;
import com.bitirme.repository.SourceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsServiceImplTest {

    @Mock
    private NewsRepository newsRepository;
    @Mock
    private SourceRepository sourceRepository;
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private NewsServiceImpl newsService;

    @Test
    @DisplayName("getById haber yoksa NotFoundException")
    void getByIdNotFoundTest() {
        when(newsRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> newsService.getById(1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("create kaynak yoksa NotFoundException")
    void createSourceMissingTest() {
        NewsCreateRequest req = new NewsCreateRequest();
        req.setSourceId(9);
        req.setTitle("Başlık");
        when(sourceRepository.findById(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> newsService.create(req))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("create aynı kaynak ve normalize başlıkta AlreadyExistsException")
    void createDuplicateNormalizedTitleTest() {
        Source src = new Source();
        src.setId(1);
        NewsCreateRequest req = new NewsCreateRequest();
        req.setSourceId(1);
        req.setTitle("Önemli ekonomi haberi");
        when(sourceRepository.findById(1)).thenReturn(Optional.of(src));
        when(newsRepository.existsBySourceIdAndNormalizedTitle(anyInt(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> newsService.create(req))
                .isInstanceOf(AlreadyExistsException.class);
    }

    @Test
    @DisplayName("create başarılı")
    void createSuccessTest() {
        Source src = new Source();
        src.setId(1);
        NewsCreateRequest req = new NewsCreateRequest();
        req.setSourceId(1);
        req.setTitle("Benzersiz başlık xyz123");
        req.setContent("içerik");
        when(sourceRepository.findById(1)).thenReturn(Optional.of(src));
        when(newsRepository.existsBySourceIdAndNormalizedTitle(anyInt(), anyString())).thenReturn(false);
        when(newsRepository.save(any(News.class))).thenAnswer(inv -> {
            News n = inv.getArgument(0);
            n.setId(10L);
            return n;
        });

        NewsResponse r = newsService.create(req);

        assertThat(r.getId()).isEqualTo(10L);
        verify(newsRepository).save(any(News.class));
    }
}
