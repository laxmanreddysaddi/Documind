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

            // ✅ FINAL FIX (NO ERROR + FINAL VARIABLE)
            final String cleanQuestion = normalizeText(
                    question == null ? "" : question.toLowerCase().trim()
            );

            System.out.println("❓ Question: " + cleanQuestion);

            // ================= QUERY EMBEDDING =================
            float[] queryVector = embeddingService.embed(cleanQuestion).vector();

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

                        String text = e.getChunkText().toLowerCase();

                        // 🔥 STRONG MATCH BOOST
                        if (text.contains(cleanQuestion)) {
                            score += 0.3f;
                        }

                        // 🔥 PARTIAL MATCH BOOST
                        for (String word : cleanQuestion.split(" ")) {
                            if (text.contains(word)) {
                                score += 0.05f;
                            }
                        }

                        return new ScoredChunk(e.getChunkText(), score);
                    })
                    .sorted((a, b) -> Float.compare(b.score, a.score))
                    .toList();

            float maxScore = scoredChunks.get(0).score;
            System.out.println("🔥 Max Score: " + maxScore);

            // ================= TOP 3 CONTEXT =================
            List<ScoredChunk> topChunks = scoredChunks.stream()
                    .limit(3)
                    .toList();

            String context = topChunks.stream()
                    .map(c -> c.text)
                    .collect(Collectors.joining("\n\n"));

            System.out.println("📄 FINAL CONTEXT:\n" + context);

            // ================= PROMPT =================
            String prompt =
                    "You are DocuMind AI.\n\n" +
                    "Answer using ONLY the given context.\n" +
                    "Be clear and short.\n" +
                    "If partial information exists, still answer.\n\n" +
                    "Context:\n" + context +
                    "\n\nQuestion:\n" + cleanQuestion +
                    "\n\nAnswer:";

            String answer = chatModel.generate(prompt);

            if (answer == null || answer.trim().isEmpty()) {
                return "Not found in document";
            }

            return answer.trim();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    // ================= NORMALIZE TEXT =================
    private String normalizeText(String text) {

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {

            if (i > 1 &&
                text.charAt(i) == text.charAt(i - 1) &&
                text.charAt(i) == text.charAt(i - 2)) {
                continue;
            }

            result.append(text.charAt(i));
        }

        return result.toString();
    }

    // ================= COSINE SIMILARITY =================
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