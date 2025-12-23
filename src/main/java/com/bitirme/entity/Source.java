package com.bitirme.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sources")
@Getter
@Setter
public class Source {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "base_url")
    private String baseUrl;

    @Column(name = "category_path")
    private String categoryPath;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "crawl_url", length = 500)
    private String crawlUrl;

    @Column(name = "title_selector", length = 200)
    private String titleSelector;

    @Column(name = "content_selector", length = 200)
    private String contentSelector;

    @Column(name = "link_selector", length = 200)
    private String linkSelector;

    @Column(name = "crawl_type", length = 50)
    private String crawlType; // "general" or "breaking_news"

    @Column(name = "last_minute_url", length = 500)
    private String lastMinuteUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

