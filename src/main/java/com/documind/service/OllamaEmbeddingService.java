package com.documind.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OllamaEmbeddingService {

    private final OllamaEmbeddingModel embeddingModel;
    private final EmbeddingCacheService cacheService;

    private static final Logger log =
            LoggerFactory.getLogger(OllamaEmbeddingService.class);

    public OllamaEmbeddingService(EmbeddingCacheService cacheService) {

        this.cacheService = cacheService;

        this.embeddingModel = OllamaEmbeddingModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("nomic-embed-text")
                .build();
    }

    public Embedding embed(String text) {

        if (text == null || text.isBlank()) {
            text = "empty";
        }

        // check cache
        Embedding cached = cacheService.get(text);

        if (cached != null) {
            log.info("Embedding Cache HIT");
            return cached;
        }

        log.info("Embedding Cache MISS - Generating new embedding");

        Embedding embedding =
                embeddingModel.embed(TextSegment.from(text)).content();

        cacheService.put(text, embedding);

        return embedding;
    }
}