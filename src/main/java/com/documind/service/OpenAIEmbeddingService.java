package com.documind.service;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;

import org.springframework.stereotype.Service;

@Service
public class OpenAIEmbeddingService {

    private final EmbeddingModel model;

    public OpenAIEmbeddingService() {
        this.model = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("text-embedding-3-small") // 🔥 important
                .build();
    }

    public Embedding embed(String text) {
        return model.embed(text).content();
    }
}