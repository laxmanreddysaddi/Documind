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

        try {
            List<Document> docs = documentRepository.findByUserUsername("testuser");

            if (docs.isEmpty()) return "⚠ No documents uploaded.";

            List<Long> docIds = docs.stream()
                    .map(Document::getId)
                    .toList();

            List<DocumentEmbedding> embeddings =
                    embeddingRepository.findByDocumentIdIn(docIds);

            if (embeddings.isEmpty()) return "⚠ No embeddings found.";

            List<String> chunks = embeddings.stream()
                    .map(DocumentEmbedding::getChunkText)
                    .limit(3)
                    .collect(Collectors.toList());

            String context = String.join("\n\n", chunks);

            String prompt =
                    "Answer only from context.\n" +
                    "If not found say 'Not found in document'.\n\n" +
                    "Context:\n" + context +
                    "\nQuestion:\n" + question;

            return chatModel.generate(prompt);

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error";
        }
    }
}