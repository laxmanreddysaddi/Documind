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

        final String cleanQuestion = normalizeText(
                question == null ? "" : question.toLowerCase().trim()
        );

        System.out.println("❓ Question: " + cleanQuestion);

        List<DocumentEmbedding> embeddings =
                embeddingRepository.findByDocumentId(documentId);

        if (embeddings.isEmpty()) {
            return "Not found in document";
        }

        // ================= STEP 1: FILTER BY KEYWORD =================
        List<DocumentEmbedding> matched = embeddings.stream()
                .filter(e -> {
                    String text = e.getChunkText().toLowerCase();

                    if (text.contains(cleanQuestion)) return true;

                    for (String word : cleanQuestion.split(" ")) {
                        if (text.contains(word)) return true;
                    }
                    return false;
                })
                .toList();

        if (matched.isEmpty()) {
            return "Not found in document";
        }

        // ================= STEP 2: RANK USING EMBEDDING =================
        float[] queryVector = embeddingService.embed(cleanQuestion).vector();

        List<ScoredChunk> ranked = matched.stream()
                .map(e -> new ScoredChunk(
                        e.getChunkText(),
                        cosineSimilarity(queryVector, stringToVector(e.getEmbedding()))
                ))
                .sorted((a, b) -> Float.compare(b.score, a.score))
                .toList();

        // ================= STEP 3: TAKE BEST 2 =================
        List<String> bestChunks = ranked.stream()
                .limit(2)   // 🔥 VERY IMPORTANT (less noise)
                .map(c -> c.text)
                .toList();

        String context = String.join("\n\n", bestChunks);

        System.out.println("📄 FINAL CONTEXT:\n" + context);

        // ================= PROMPT =================
        String prompt =
                "You are DocuMind AI.\n\n" +
                "Answer ONLY from the context.\n" +
                "Give direct answer.\n" +
                "Do NOT include unrelated content.\n\n" +
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