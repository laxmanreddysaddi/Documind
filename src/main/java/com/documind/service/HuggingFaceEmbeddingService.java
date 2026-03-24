package com.documind.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import org.springframework.stereotype.Service;

@Service
public class HuggingFaceEmbeddingService {

    private final EmbeddingModel model;

    public HuggingFaceEmbeddingService() {

        String token = System.getenv("HF_TOKEN");

        if (token == null || token.isEmpty()) {
            throw new RuntimeException("❌ HF_TOKEN is not set in environment variables");
        }

        this.model = HuggingFaceEmbeddingModel.builder()
                .accessToken(token)
                .modelId("sentence-transformers/all-MiniLM-L6-v2")
                .build();
    }

    public Embedding embed(String text) {

        try {
            return model.embed(text).content();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ HuggingFace embedding failed");
        }
    }
}