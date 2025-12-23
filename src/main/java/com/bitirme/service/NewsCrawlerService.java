package com.bitirme.service;

import com.bitirme.entity.Source;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsCrawlerService {

    private final DynamicNewsCrawlerService dynamicNewsCrawlerService;

    @Transactional
    public int crawlAllSources() {
        return dynamicNewsCrawlerService.crawlAllSources();
    }

    @Transactional
    public int crawlSource(Source source) {
        return dynamicNewsCrawlerService.crawlSource(source);
    }

    @Transactional
    public int crawlBreakingNews() {
        return dynamicNewsCrawlerService.crawlBreakingNews();
    }
}
