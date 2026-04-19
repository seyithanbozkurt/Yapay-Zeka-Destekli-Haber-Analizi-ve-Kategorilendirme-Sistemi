package com.bitirme.service;

import com.bitirme.repository.CrawlingLogRepository;
import com.bitirme.repository.NewsRepository;
import com.bitirme.repository.SourceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamicNewsCrawlerServiceTest {

    @Mock
    private NewsRepository newsRepository;
    @Mock
    private SourceRepository sourceRepository;
    @Mock
    private NewsService newsService;
    @Mock
    private CrawlingLogRepository crawlingLogRepository;

    @InjectMocks
    private DynamicNewsCrawlerService dynamicNewsCrawlerService;

    @Test
    @DisplayName("crawlAllSources aktif kaynak yoksa 0 döner")
    void crawlAllSourcesNoActiveSourcesTest() {
        when(sourceRepository.findByActiveTrue()).thenReturn(Collections.emptyList());

        int total = dynamicNewsCrawlerService.crawlAllSources();

        assertThat(total).isZero();
    }
}
