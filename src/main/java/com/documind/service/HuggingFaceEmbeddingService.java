package com.documind.service;

import dev.langchain4j.data.embedding.Embedding;
import org.springframework.stereotype.Service;

@Service
public class HuggingFaceEmbeddingService {

    // ✅ Simple local embedding (hash-based)
    public Embedding embed(String text) {

        float[] vector = new float[100];

        for (int i = 0; i < text.length(); i++) {
            vector[i % 100] += text.charAt(i);
        }

        return new Embedding(vector);
    }
}