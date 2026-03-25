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
                .modelId("intfloat/e5-small-v2") // ✅ WORKING MODEL
                .build();
    }

    public Embedding embed(String text) {
        try {
            System.out.println("⚡ Calling HuggingFace embedding API...");
            return model.embed(text).content();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ HuggingFace embedding failed: " + e.getMessage());
        }
    }
}