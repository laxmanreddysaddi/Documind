package com.documind.config;

import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VectorStoreConfig {

    @Bean
    public InMemoryEmbeddingStore<TextSegment> vectorStore() {
        return new InMemoryEmbeddingStore<>();
    }
}