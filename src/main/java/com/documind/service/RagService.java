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

            // ================= 1. QUERY EMBEDDING =================
            float[] queryVector = embeddingService.embed(question).vector();

            // ================= 2. FETCH EMBEDDINGS =================
            List<DocumentEmbedding> embeddings =
                    embeddingRepository.findByDocumentId(documentId);

            System.out.println("📊 Embeddings: " + embeddings.size());

            if (embeddings.isEmpty()) {
                return "Not found in document";
            }

            // ================= 3. CALCULATE SIMILARITY =================
            List<ScoredChunk> scoredChunks = embeddings.stream()
                    .map(e -> new ScoredChunk(
                            e.getChunkText(),
                            cosineSimilarity(queryVector, stringToVector(e.getEmbedding()))
                    ))
                    .sorted((a, b) -> Float.compare(b.score, a.score))
                    .collect(Collectors.toList());

            // ================= 4. FILTER STRICT =================
            List<String> topChunks = scoredChunks.stream()
                    .filter(c -> c.score > 0.75) // 🔥 STRICT THRESHOLD
                    .limit(3)
                    .map(c -> c.text)
                    .collect(Collectors.toList());

            if (topChunks.isEmpty()) {
                return "Not found in document";
            }

            // ================= 5. BUILD CONTEXT =================
            String context = String.join("\n\n", topChunks);

            System.out.println("📄 Context used:\n" + context);

            // ================= 6. STRICT PROMPT =================
            String prompt =
                    "You are DocuMind AI.\n\n" +
                    "STRICT RULES:\n" +
                    "1. Answer ONLY using the given context.\n" +
                    "2. DO NOT use outside knowledge.\n" +
                    "3. If answer is not in context, reply EXACTLY: Not found in document.\n\n" +
                    "CONTEXT:\n" + context +
                    "\n\nQUESTION:\n" + question +
                    "\n\nANSWER:";

            String answer = chatModel.generate(prompt);

            // ================= 7. FINAL SAFETY CHECK =================
            if (answer == null || answer.trim().isEmpty()) {
                return "Not found in document";
            }

            return answer;

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error: " + e.getMessage();
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

    // ================= HELPER CLASS =================
    static class ScoredChunk {
        String text;
        float score;

        ScoredChunk(String text, float score) {
            this.text = text;
            this.score = score;
        }
    }
}