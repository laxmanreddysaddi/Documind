package com.documind.service;

import com.documind.model.ChatHistory;
import com.documind.model.Document;
import com.documind.model.DocumentEmbedding;
import com.documind.model.User;
import com.documind.repository.ChatHistoryRepository;
import com.documind.repository.DocumentEmbeddingRepository;
import com.documind.repository.DocumentRepository;
import com.documind.repository.UserRepository;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final HuggingFaceEmbeddingService embeddingService;
    private final DocumentEmbeddingRepository embeddingRepository;
    private final DocumentRepository documentRepository;
    private final ChatHistoryRepository chatHistoryRepository;
    private final UserRepository userRepository;
    private final ChatLanguageModel chatModel;

    public RagService(
            HuggingFaceEmbeddingService embeddingService,
            DocumentEmbeddingRepository embeddingRepository,
            DocumentRepository documentRepository,
            ChatLanguageModel chatModel,
            ChatHistoryRepository chatHistoryRepository,
            UserRepository userRepository
    ) {
        this.embeddingService = embeddingService;
        this.embeddingRepository = embeddingRepository;
        this.documentRepository = documentRepository;
        this.chatModel = chatModel;
        this.chatHistoryRepository = chatHistoryRepository;
        this.userRepository = userRepository;
    }

    public String ask(String question, String username) {

        User user = userRepository.findByUsername(username).orElse(null);

        try {
            // 1️⃣ Generate query embedding
            var queryEmbedding = embeddingService.embed(question);
            float[] queryVector = queryEmbedding.vector();

            // 2️⃣ Get user's documents
            List<Document> docs = documentRepository.findByUserUsername(username);

            if (docs.isEmpty()) {
                return "⚠ No documents uploaded.";
            }

            List<Long> docIds = docs.stream()
                    .map(Document::getId)
                    .toList();

            // 3️⃣ Get embeddings
            List<DocumentEmbedding> embeddings =
                    embeddingRepository.findByDocumentIdIn(docIds);

            if (embeddings.isEmpty()) {
                return "⚠ No embeddings found.";
            }

            // 4️⃣ Similarity search
            List<String> topChunks = embeddings.stream()
                    .sorted((a, b) -> {
                        float simA = similarity(queryVector, stringToVector(a.getEmbedding()));
                        float simB = similarity(queryVector, stringToVector(b.getEmbedding()));
                        return Float.compare(simB, simA);
                    })
                    .limit(3)
                    .map(DocumentEmbedding::getChunkText)
                    .collect(Collectors.toList());

            String context = String.join("\n\n", topChunks);

            String prompt =
                    "Answer ONLY from the context.\n" +
                    "If not found, say 'Not found in document'.\n\n" +
                    "Context:\n" + context +
                    "\n\nQuestion:\n" + question +
                    "\nAnswer:";

            String answer = chatModel.generate(prompt);

            // Save history
            if (user != null) {
                ChatHistory chat = new ChatHistory();
                chat.setQuestion(question);
                chat.setAnswer(answer);
                chat.setTimestamp(LocalDateTime.now());
                chat.setUser(user);
                chatHistoryRepository.save(chat);
            }

            return answer;

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error processing request";
        }
    }

    private float similarity(float[] v1, float[] v2) {
        float sum = 0;
        for (int i = 0; i < v1.length; i++) {
            sum += v1[i] * v2[i];
        }
        return sum;
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