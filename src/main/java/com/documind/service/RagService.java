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
            System.out.println("🔥 RAG STARTED");

            // ✅ 1. Query embedding
            float[] queryVector = embeddingService.embed(question).vector();

            // ✅ 2. Get user documents
            List<Document> docs = documentRepository.findByUserUsername(username);

            if (docs.isEmpty()) {
                return "⚠ Please upload a document first.";
            }

            List<Long> docIds = docs.stream().map(Document::getId).toList();
            System.out.println("📄 User Doc IDs: " + docIds);

            // ✅ 3. Fetch embeddings (FIXED)
            List<DocumentEmbedding> embeddings =
                    embeddingRepository.findByDocumentIdIn(docIds);

            System.out.println("📊 Embeddings fetched: " + embeddings.size());

            if (embeddings.isEmpty()) {
                return "⚠ No embeddings found.";
            }

            // ✅ 4. Find top chunks
            List<String> topChunks = embeddings.stream()
                    .sorted((a, b) -> Float.compare(
                            cosineSimilarity(queryVector, stringToVector(b.getEmbedding())),
                            cosineSimilarity(queryVector, stringToVector(a.getEmbedding()))
                    ))
                    .limit(3)
                    .map(DocumentEmbedding::getChunkText)
                    .toList();

            // ✅ 5. Build context
            StringBuilder context = new StringBuilder();
            for (String chunk : topChunks) {
                context.append(chunk).append("\n\n");
            }

            // ✅ STRICT PROMPT
            String prompt =
                    "You are DocuMind AI.\n\n" +
                    "STRICT RULES:\n" +
                    "1. Answer ONLY from the given context.\n" +
                    "2. Do NOT use outside knowledge.\n" +
                    "3. If not found, say 'Not found in document'.\n\n" +
                    "CONTEXT:\n" + context +
                    "\nQUESTION:\n" + question +
                    "\nANSWER:";

            return chatModel.generate(prompt);

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error: " + e.getMessage();
        }
    }

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