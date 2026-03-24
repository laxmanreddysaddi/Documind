package com.documind.service;

import com.documind.model.ChatHistory;
import com.documind.model.User;
import com.documind.repository.ChatHistoryRepository;
import com.documind.repository.DocumentEmbeddingRepository;
import com.documind.repository.UserRepository;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RagService {

    private final HuggingFaceEmbeddingService embeddingService;
    private final DocumentEmbeddingRepository embeddingRepository;
    private final ChatHistoryRepository chatHistoryRepository;
    private final UserRepository userRepository;
    private final ChatLanguageModel chatModel;

    public RagService(
            HuggingFaceEmbeddingService embeddingService,
            DocumentEmbeddingRepository embeddingRepository,
            ChatLanguageModel chatModel,
            ChatHistoryRepository chatHistoryRepository,
            UserRepository userRepository
    ) {
        this.embeddingService = embeddingService;
        this.embeddingRepository = embeddingRepository;
        this.chatModel = chatModel;
        this.chatHistoryRepository = chatHistoryRepository;
        this.userRepository = userRepository;
    }

    public String ask(String question, String username) {

        System.out.println("🔥 Chat API called");

        // 1️⃣ Get user
        User user = userRepository.findByUsername(username).orElse(null);

        // 2️⃣ VECTOR SEARCH
        List<String> topChunks;

        try {
            var embedding = embeddingService.embed(question);

            float[] vector = embedding.vector();

            String vectorString = convertToVectorString(vector);

            topChunks = embeddingRepository.findTop3SimilarByUser(vectorString);

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error in vector search";
        }

        // 🚨 PROCESSING STATE
        if (topChunks == null || topChunks.isEmpty()) {
            return "⚠ Document is still processing. Please try again.";
        }

        // 3️⃣ Build context
        StringBuilder context = new StringBuilder();

        for (String chunk : topChunks) {
            context.append(chunk).append("\n\n");
        }

        // 4️⃣ Prompt
        String prompt =
"""
You are DocuMind AI.

Rules:
- Answer clearly in bullet points
- Use only the provided context
- Do not mention sources

Context:
""" + context +

"\nQuestion:\n" + question +
"\nAnswer:";

        // 5️⃣ LLM
        String answer = chatModel.generate(prompt);

        // 6️⃣ Save history
        if (user != null) {
            ChatHistory chat = new ChatHistory();
            chat.setQuestion(question);
            chat.setAnswer(answer);
            chat.setTimestamp(LocalDateTime.now());
            chat.setUser(user);
            chatHistoryRepository.save(chat);
        }

        return answer;
    }

    private String convertToVectorString(float[] vector) {

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        sb.append("]");

        return sb.toString();
    }
}