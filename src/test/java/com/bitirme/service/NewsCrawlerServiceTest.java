package com.bitirme.service;

import com.bitirme.entity.Source;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsCrawlerServiceTest {

    @Mock
    private DynamicNewsCrawlerService dynamicNewsCrawlerService;

    @InjectMocks
    private NewsCrawlerService newsCrawlerService;

    @Test
    @DisplayName("crawlAllSources DynamicNewsCrawlerService'e delege eder")
    void crawlAllSourcesDelegatesTest() {
        when(dynamicNewsCrawlerService.crawlAllSources()).thenReturn(3);

        int n = newsCrawlerService.crawlAllSources();

        assertThat(n).isEqualTo(3);
        verify(dynamicNewsCrawlerService).crawlAllSources();
    }

    @Test
    @DisplayName("crawlSource delege eder")
    void crawlSourceDelegatesTest() {
        Source s = new Source();
        s.setId(1);
        when(dynamicNewsCrawlerService.crawlSource(s)).thenReturn(2);

        assertThat(newsCrawlerService.crawlSource(s)).isEqualTo(2);
        verify(dynamicNewsCrawlerService).crawlSource(s);
    }

    @Test
    @DisplayName("crawlBreakingNews delege eder")
    void crawlBreakingNewsDelegatesTest() {
        when(dynamicNewsCrawlerService.crawlBreakingNews()).thenReturn(1);

        assertThat(newsCrawlerService.crawlBreakingNews()).isEqualTo(1);
        verify(dynamicNewsCrawlerService).crawlBreakingNews();
    }
}
