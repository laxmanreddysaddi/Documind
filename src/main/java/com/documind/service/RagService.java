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

        System.out.println("🔥 RAG STARTED");
        System.out.println("User Query: " + question);

        User user = userRepository.findByUsername(username).orElse(null);

        List<String> topChunks;

        try {
            System.out.println("⚡ Generating embedding...");
            var embedding = embeddingService.embed(question);

            float[] vector = embedding.vector();
            String vectorString = convertToVectorString(vector);

            System.out.println("⚠ Using fallback vector search...");

            // ✅ TEMP SAFE DATA (NO CRASH)
            topChunks = embeddingRepository.findTop3SimilarByUser(vectorString);

            System.out.println("📄 Results: " + topChunks.size());

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Vector error: " + e.getMessage();
        }

        if (topChunks == null || topChunks.isEmpty()) {
            return "⚠ No documents found. Please upload a file.";
        }

        StringBuilder context = new StringBuilder();
        for (String chunk : topChunks) {
            context.append(chunk).append("\n\n");
        }

        String prompt =
                "You are DocuMind AI.\n\n" +
                "Rules:\n" +
                "- Answer clearly in bullet points\n" +
                "- Use only the provided context\n\n" +
                "Context:\n" + context +
                "\nQuestion:\n" + question +
                "\nAnswer:";

                String answer;

         try {
    System.out.println("🤖 Calling OpenRouter...");
    answer = chatModel.generate(prompt);
    System.out.println("✅ AI Response received");

} catch (Exception e) {
    e.printStackTrace();
    return "❌ Chat error: " + e.getMessage();
}
        
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