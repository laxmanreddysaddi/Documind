package com.documind.service;

import com.documind.model.Document;
import com.documind.model.DocumentEmbedding;
import com.documind.repository.DocumentEmbeddingRepository;
import com.documind.repository.DocumentRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagService {

    private final HuggingFaceEmbeddingService embeddingService;
    private final DocumentEmbeddingRepository embeddingRepository;
    private final DocumentRepository documentRepository;
    private final ChatLanguageModel chatModel;

    public RagService(
            HuggingFaceEmbeddingService embeddingService,
            DocumentEmbeddingRepository embeddingRepository,
            DocumentRepository documentRepository,
            ChatLanguageModel chatModel
    ) {
        this.embeddingService = embeddingService;
        this.embeddingRepository = embeddingRepository;
        this.documentRepository = documentRepository;
        this.chatModel = chatModel;
    }

    public String ask(String question, String username) {

        try {
            System.out.println("\n🔥 ===== RAG STARTED =====");
            System.out.println("❓ Question: " + question);

            // =========================
            // 1️⃣ Generate query embedding
            // =========================
            float[] queryVector = embeddingService.embed(question).vector();

            // =========================
            // 2️⃣ Fetch user documents
            // =========================
            List<Document> docs = documentRepository.findByUserUsername(username);

            if (docs.isEmpty()) {
                return "⚠ Please upload a document first.";
            }

            List<Long> docIds = docs.stream()
                    .map(Document::getId)
                    .toList();

            System.out.println("📄 User Doc IDs: " + docIds);

            // =========================
            // 3️⃣ Fetch embeddings
            // =========================
            List<DocumentEmbedding> embeddings =
                    embeddingRepository.findByDocumentIdIn(docIds);

            System.out.println("📊 Embeddings fetched: " + embeddings.size());

            if (embeddings.isEmpty()) {
                return "⚠ Document processed but no embeddings found.";
            }

            // =========================
            // 4️⃣ Rank by similarity
            // =========================
            List<String> topChunks = embeddings.stream()
                    .filter(e -> e.getEmbedding() != null)
                    .sorted((a, b) -> Float.compare(
                            cosineSimilarity(queryVector, stringToVector(b.getEmbedding())),
                            cosineSimilarity(queryVector, stringToVector(a.getEmbedding()))
                    ))
                    .limit(3)
                    .map(DocumentEmbedding::getChunkText)
                    .toList();

            if (topChunks.isEmpty()) {
                return "⚠ No relevant content found.";
            }

            // =========================
            // 5️⃣ Build context
            // =========================
            StringBuilder context = new StringBuilder();
            for (String chunk : topChunks) {
                context.append(chunk).append("\n\n");
            }

            System.out.println("📚 Context built");

            // =========================
            // 6️⃣ Strict prompt
            // =========================
            String prompt =
                    "You are DocuMind AI.\n\n" +
                    "STRICT RULES:\n" +
                    "1. Answer ONLY from the given context.\n" +
                    "2. Do NOT use outside knowledge.\n" +
                    "3. If answer is NOT in context, reply EXACTLY: Not found in document\n" +
                    "4. Keep answer short and clear.\n\n" +

                    "CONTEXT:\n" + context + "\n\n" +
                    "QUESTION:\n" + question + "\n\n" +
                    "ANSWER:";

            // =========================
            // 7️⃣ Generate answer
            // =========================
            String response = chatModel.generate(prompt);

            System.out.println("✅ Answer generated");

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error: " + e.getMessage();
        }
    }

    // =========================
    // Cosine Similarity
    // =========================
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

    // =========================
    // String → Vector
    // =========================
    private float[] stringToVector(String str) {

        if (str == null || str.isEmpty()) return new float[0];

        str = str.replace("[", "").replace("]", "");

        String[] parts = str.split(",");

        float[] vector = new float[parts.length];

        for (int i = 0; i < parts.length; i++) {
            try {
                vector[i] = Float.parseFloat(parts[i]);
            } catch (Exception e) {
                vector[i] = 0;
            }
        }

        return vector;
    }
}