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
            float[] queryVector = embeddingService.embed(question).vector();

            List<DocumentEmbedding> embeddings =
                    embeddingRepository.findByDocumentId(documentId);

            if (embeddings.isEmpty()) {
                return "Not found in document";
            }

            List<ScoredChunk> scored = embeddings.stream()
                    .map(e -> new ScoredChunk(
                            e.getChunkText(),
                            cosineSimilarity(queryVector, stringToVector(e.getEmbedding()))
                    ))
                    .sorted((a, b) -> Float.compare(b.score, a.score))
                    .collect(Collectors.toList());

            float maxScore = scored.get(0).score;

            float threshold = Math.max(0.7f, maxScore - 0.05f);

            List<String> topChunks = scored.stream()
                    .filter(c -> c.score >= threshold)
                    .limit(3)
                    .map(c -> c.text)
                    .collect(Collectors.toList());

            if (topChunks.isEmpty()) {
                return "Not found in document";
            }

            String context = topChunks.stream()
                    .filter(s -> s.length() > 20)
                    .collect(Collectors.joining("\n\n"));

            String prompt =
                    "Answer ONLY from context.\n" +
                    "If not present → reply: Not found in document.\n\n" +
                    "Context:\n" + context +
                    "\n\nQuestion:\n" + question;

            String answer = chatModel.generate(prompt);

            if (answer == null || answer.trim().isEmpty()) {
                return "Not found in document";
            }

            return answer;

        } catch (Exception e) {
            e.printStackTrace();
            return "Error";
        }
    }

    private float cosineSimilarity(float[] a, float[] b) {
        float dot = 0, normA = 0, normB = 0;

        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0 || normB == 0) return 0;

        return (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }

    private float[] stringToVector(String str) {
        str = str.replace("[", "").replace("]", "");
        String[] parts = str.split(",");

        float[] vector = new float[parts.length];

        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i]);
        }

        return vector;
    }

    static class ScoredChunk {
        String text;
        float score;

        ScoredChunk(String text, float score) {
            this.text = text;
            this.score = score;
        }
    }
}