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
            // 1️⃣ Query embedding
            float[] queryVector = embeddingService.embed(question).vector();

            // 2️⃣ Get documents
            List<Document> docs = documentRepository.findByUserUsername(username);

            if (docs.isEmpty()) {
                return "⚠ Please upload a document first.";
            }

            List<Long> docIds = docs.stream().map(Document::getId).toList();

            // 3️⃣ Get embeddings
            List<DocumentEmbedding> embeddings = embeddingRepository.findAll();

            if (embeddings.isEmpty()) {
                return "⚠ No embeddings found.";
            }

            // 4️⃣ Find top chunks
            List<String> topChunks = embeddings.stream()
                    .sorted((a, b) -> Float.compare(
                            cosineSimilarity(queryVector, stringToVector(b.getEmbedding())),
                            cosineSimilarity(queryVector, stringToVector(a.getEmbedding()))
                    ))
                    .limit(3)
                    .map(DocumentEmbedding::getChunkText)
                    .toList();

            // 5️⃣ Context
            StringBuilder context = new StringBuilder();
            for (String chunk : topChunks) {
                context.append(chunk).append("\n\n");
            }

            String prompt =
                    "Answer only from context:\n\n" +
                    context +
                    "\nQuestion: " + question;

            return chatModel.generate(prompt);

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error: " + e.getMessage();
        }
    }

    private float cosineSimilarity(float[] a, float[] b) {
        float dot = 0, normA = 0, normB = 0;

        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        return (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }

    private float[] stringToVector(String str) {
        str = str.replace("[", "").replace("]", "");
        String[] parts = str.split(",");

        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i]);
        }

        System.out.println("📄 Doc IDs: " + docIds);
System.out.println("📊 Total embeddings in DB: " + embeddingRepository.count());
System.out.println("📊 Embeddings fetched: " + embeddings.size());
        return vector;
    }
}