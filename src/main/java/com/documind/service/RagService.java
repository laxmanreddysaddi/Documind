package com.documind.service;

import com.documind.model.DocumentEmbedding;
import com.documind.repository.DocumentEmbeddingRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final HuggingFaceEmbeddingService embeddingService;
    private final DocumentEmbeddingRepository embeddingRepository;
    private final ChatLanguageModel chatModel;

    public RagService(
            HuggingFaceEmbeddingService embeddingService,
            DocumentEmbeddingRepository embeddingRepository,
            ChatLanguageModel chatModel
    ) {
        this.embeddingService = embeddingService;
        this.embeddingRepository = embeddingRepository;
        this.chatModel = chatModel;
    }

    public String ask(String question, Long documentId) {

        try {
            System.out.println("🔥 ===== RAG STARTED =====");
            System.out.println("❓ Question: " + question);

            // ========= 1. EMBED QUESTION =========
            float[] queryVector = embeddingService.embed(question).vector();

            // ========= 2. FETCH DOCUMENT CHUNKS =========
            List<DocumentEmbedding> embeddings =
                    embeddingRepository.findByDocumentId(documentId);

            System.out.println("📊 Embeddings: " + embeddings.size());

            if (embeddings.isEmpty()) {
                return "Not found in document";
            }

            // ========= 3. SCORE CHUNKS =========
            List<ScoredChunk> scoredChunks = embeddings.stream()
                    .map(e -> new ScoredChunk(
                            e.getChunkText(),
                            cosineSimilarity(queryVector, stringToVector(e.getEmbedding()))
                    ))
                    .sorted((a, b) -> Float.compare(b.score, a.score))
                    .collect(Collectors.toList());

            // ========= 4. DYNAMIC THRESHOLD =========
            float maxScore = scoredChunks.get(0).score;
            float threshold = Math.max(0.60f, maxScore - 0.10f);

            System.out.println("🔥 Max Score: " + maxScore);
            System.out.println("🔥 Threshold: " + threshold);

            List<String> topChunks = scoredChunks.stream()
                    .filter(c -> c.score >= threshold)
                    .limit(3)
                    .map(c -> c.text)
                    .collect(Collectors.toList());

            if (topChunks.isEmpty()) {
                return "Not found in document";
            }

            // ========= 5. BUILD CONTEXT =========
            String context = String.join("\n\n", topChunks);

            System.out.println("📄 Context:\n" + context);

            // ========= 6. ULTRA STRICT PROMPT =========
            String prompt =
                    "You are a STRICT document question-answering system.\n\n" +

                    "RULES:\n" +
                    "1. Answer ONLY using the provided context.\n" +
                    "2. DO NOT use any outside knowledge.\n" +
                    "3. DO NOT guess or assume anything.\n" +
                    "4. If answer is not clearly present, reply EXACTLY: Not found in document\n" +
                    "5. Keep answer short and precise.\n\n" +

                    "CONTEXT:\n" + context +

                    "\n\nQUESTION:\n" + question +

                    "\n\nFINAL ANSWER:";

            String answer = chatModel.generate(prompt);

            // ========= 7. FINAL SAFETY =========
            if (answer == null || answer.trim().isEmpty()) {
                return "Not found in document";
            }

            // 🔥 HARD FILTER (ANTI-HALLUCINATION)
            if (!context.toLowerCase().contains(answer.toLowerCase().substring(0, Math.min(20, answer.length())))) {
                return "Not found in document";
            }

            return answer.trim();

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error in RAG";
        }
    }

    // ========= COSINE SIMILARITY =========
    private float cosineSimilarity(float[] a, float[] b) {

        int length = Math.min(a.length, b.length);

        float dot = 0, normA = 0, normB = 0;

        for (int i = 0; i < length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0 || normB == 0) return 0;

        return (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }

    // ========= STRING → VECTOR =========
    private float[] stringToVector(String str) {

        str = str.replace("[", "").replace("]", "");
        String[] parts = str.split(",");

        float[] vector = new float[parts.length];

        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i]);
        }

        return vector;
    }

    // ========= HELPER =========
    static class ScoredChunk {
        String text;
        float score;

        ScoredChunk(String text, float score) {
            this.text = text;
            this.score = score;
        }
    }
}