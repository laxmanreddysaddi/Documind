package com.documind.service;

import com.documind.model.Document;
import com.documind.model.DocumentEmbedding;
import com.documind.repository.DocumentEmbeddingRepository;
import com.documind.repository.DocumentRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final DocumentEmbeddingRepository embeddingRepository;
    private final DocumentRepository documentRepository;
    private final ChatLanguageModel chatModel;

    public RagService(
            DocumentEmbeddingRepository embeddingRepository,
            DocumentRepository documentRepository,
            ChatLanguageModel chatModel
    ) {
        this.embeddingRepository = embeddingRepository;
        this.documentRepository = documentRepository;
        this.chatModel = chatModel;
    }

  public String ask(String question, String username) {

    System.out.println("🔥 RAG STARTED: " + question);

    try {

        // 1️⃣ Get user documents
        List<Document> docs = documentRepository.findByUserUsername(username);

        if (docs.isEmpty()) {
            return "⚠ Please upload a document first.";
        }

        List<Long> docIds = docs.stream()
                .map(Document::getId)
                .toList();

        // 2️⃣ Get embeddings
        List<DocumentEmbedding> embeddings =
                embeddingRepository.findByDocumentIdIn(docIds);

        if (embeddings.isEmpty()) {
            return "⚠ Document processed but no embeddings found.";
        }

        // 3️⃣ Build context (NO VECTOR SEARCH TEMP)
        StringBuilder context = new StringBuilder();

        for (DocumentEmbedding e : embeddings) {
            context.append(e.getChunkText()).append("\n\n");
        }

        // 4️⃣ Prompt
        String prompt =
                "Answer only from the context.\n\n" +
                context +
                "\nQuestion: " + question;

        // 5️⃣ Call AI safely
        try {
            return chatModel.generate(prompt);
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ AI model failed";
        }

    } catch (Exception e) {
        e.printStackTrace();
        return "❌ Backend crash: " + e.getMessage();
    }
}
}