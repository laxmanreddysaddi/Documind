package com.documind.service;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import org.springframework.stereotype.Service;

@Service
public class OllamaEmbeddingService {
    private final EmbeddingModel model;

    public OllamaEmbeddingService() {
        this.model = OllamaEmbeddingModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("nomic-embed-text")
                .build();
    }

    public Embedding embed(String text) {
        return model.embed(text).content();
    }
}