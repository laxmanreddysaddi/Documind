package com.documind.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.data.embedding.Embedding;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class EmbeddingCacheService {

    private final Cache<String, Embedding> cache = Caffeine.newBuilder()
            .maximumSize(10_000)                 // max 10k entries
            .expireAfterWrite(30, TimeUnit.MINUTES)  // auto expire
            .build();

    public Embedding get(String text) {
        return cache.getIfPresent(text);
    }

    public void put(String text, Embedding embedding) {
        cache.put(text, embedding);
    }

    public boolean contains(String text) {
        return cache.getIfPresent(text) != null;
    }
}