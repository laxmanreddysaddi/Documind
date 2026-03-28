package com.documind.service;

import com.documind.model.DocumentEmbedding;
import com.documind.repository.DocumentEmbeddingRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Service;

import java.util.List;

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
            System.out.println("📄 Document ID: " + documentId);

            float[] queryVector = embeddingService.embed(question).vector();

            // ✅ ONLY selected doc
            List<DocumentEmbedding> embeddings =
                    embeddingRepository.findByDocumentId(documentId);

            System.out.println("📊 Embeddings: " + embeddings.size());

            if (embeddings.isEmpty()) {
                return "❌ No embeddings found";
            }

            List<String> topChunks = embeddings.stream()
                    .map(e -> new Object[]{
                            e.getChunkText(),
                            cosineSimilarity(queryVector, stringToVector(e.getEmbedding()))
                    })
                    .filter(e -> (float)e[1] > 0.5) // 🔥 STRICT FILTER
                    .sorted((a, b) -> Float.compare((float)b[1], (float)a[1]))
                    .limit(3)
                    .map(e -> (String)e[0])
                    .toList();

            if (topChunks.isEmpty()) {
                return "Not found in document";
            }

            StringBuilder context = new StringBuilder();
            for (String chunk : topChunks) {
                context.append(chunk).append("\n\n");
            }

            String prompt =
                    "You are a strict document QA system.\n" +
                    "Answer ONLY from context.\n" +
                    "If not found say: Not found in document.\n\n" +
                    "Context:\n" + context +
                    "\nQuestion:\n" + question +
                    "\nAnswer:";

            return chatModel.generate(prompt);

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error: " + e.getMessage();
        }
    }

    private float cosineSimilarity(float[] a, float[] b) {
        int len = Math.min(a.length, b.length);

        float dot = 0, normA = 0, normB = 0;

        for (int i = 0; i < len; i++) {
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
}