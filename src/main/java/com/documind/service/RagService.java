package com.documind.service;

import com.documind.model.Document;
import com.documind.model.DocumentEmbedding;
import com.documind.repository.DocumentEmbeddingRepository;
import com.documind.repository.DocumentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagService {

    private final DocumentEmbeddingRepository embeddingRepository;
    private final DocumentRepository documentRepository;

    public RagService(
            DocumentEmbeddingRepository embeddingRepository,
            DocumentRepository documentRepository
    ) {
        this.embeddingRepository = embeddingRepository;
        this.documentRepository = documentRepository;
    }

    public String ask(String question, String username) {

        try {
            System.out.println("🔥 RAG STARTED");

            List<Document> docs = documentRepository.findByUserUsername(username);

            if (docs.isEmpty()) {
                return "⚠ Please upload a document first.";
            }

            List<Long> docIds = docs.stream().map(Document::getId).toList();

            List<DocumentEmbedding> embeddings =
                    embeddingRepository.findByDocumentIds(docIds);

            if (embeddings.isEmpty()) {
                return "⚠ No data found in document.";
            }

            // ✅ Just return top chunks (FAST & SAFE)
            StringBuilder context = new StringBuilder();

            embeddings.stream()
                    .limit(3)
                    .forEach(e -> context.append(e.getChunkText()).append("\n\n"));

            return "📄 Answer:\n\n" + context;

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error: " + e.getMessage();
        }
    }
}