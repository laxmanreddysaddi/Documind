package com.documind.service;

import com.documind.model.DocumentEmbedding;
import com.documind.repository.DocumentEmbeddingRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Service;

import java.util.List;
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

            // ================= QUERY EMBEDDING =================
            float[] queryVector = embeddingService.embed(question).vector();

            // ================= FETCH EMBEDDINGS =================
            List<DocumentEmbedding> embeddings =
                    embeddingRepository.findByDocumentId(documentId);

            if (embeddings.isEmpty()) {
                return "No data found in document";
            }

            // ================= SCORING =================
            List<ScoredChunk> scoredChunks = embeddings.stream()
                    .map(e -> {

                        float score = cosineSimilarity(
                                queryVector,
                                stringToVector(e.getEmbedding())
                        );

                        // 🔥 KEYWORD BOOST (IMPORTANT)
                        if (e.getChunkText().toLowerCase()
                                .contains(question.toLowerCase())) {
                            score += 0.1f;
                        }

                        return new ScoredChunk(e.getChunkText(), score);
                    })
                    .sorted((a, b) -> Float.compare(b.score, a.score))
                    .toList();

            float maxScore = scoredChunks.get(0).score;
            System.out.println("🔥 Max Score: " + maxScore);

            // ================= SMART THRESHOLD =================
            float threshold = Math.max(0.55f, maxScore - 0.1f);

            List<ScoredChunk> filtered = scoredChunks.stream()
                    .filter(c -> c.score >= threshold)
                    .limit(5)
                    .toList();

            // ================= FALLBACK =================
            if (filtered.isEmpty()) {
                filtered = scoredChunks.stream().limit(2).toList();
            }

            // ================= CONTEXT =================
            String context = filtered.stream()
                    .map(c -> c.text)
                    .collect(Collectors.joining("\n\n"));

            System.out.println("📄 Context:\n" + context);

            // ================= STRICT PROMPT =================
            String prompt =
                    "You are DocuMind AI.\n\n" +
                    "STRICT RULES:\n" +
                    "1. Answer ONLY from the context.\n" +
                    "2. Do NOT use outside knowledge.\n" +
                    "3. If answer is not in context, say EXACTLY: Not found in document.\n\n" +
                    "Context:\n" + context +
                    "\n\nQuestion:\n" + question +
                    "\n\nAnswer:";

            String answer = chatModel.generate(prompt);

            // ================= FINAL SAFETY =================
            if (answer == null || answer.trim().isEmpty()) {
                return "Not found in document";
            }

            return answer.trim();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    // ================= COSINE =================
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

    // ================= STRING → VECTOR =================
    private float[] stringToVector(String str) {

        str = str.replace("[", "").replace("]", "");
        String[] parts = str.split(",");

        float[] vector = new float[parts.length];

        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i]);
        }

        return vector;
    }

    // ================= HELPER =================
    static class ScoredChunk {
        String text;
        float score;

        ScoredChunk(String text, float score) {
            this.text = text;
            this.score = score;
        }
    }
}