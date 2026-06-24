package com.kuroneko.pymeflow.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "provider")
public record ProviderAuthConfig(Integer maxPages, Integer pageSize) {
    private static final int DEFAULT_MAX_PAGES = 50;
    private static final int DEFAULT_PAGE_SIZE = 100;

    public ProviderAuthConfig {
        maxPages = maxPages == null ? DEFAULT_MAX_PAGES : maxPages;
        pageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        if (maxPages <= 0) {
            throw new IllegalArgumentException("Max pages must be greater than zero");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero");
        }
    }
}
