package com.documind.service;

import dev.langchain4j.data.embedding.Embedding;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class HuggingFaceEmbeddingService {

    // ✅ TEMP: Generate fake embedding (no API call)
    public Embedding embed(String text) {

        System.out.println("⚠ Using dummy embedding (HF disabled)");

        float[] vector = new float[384]; // standard size

        Random random = new Random();
        for (int i = 0; i < vector.length; i++) {
            vector[i] = random.nextFloat();
        }

        return new Embedding(vector);
    }
}