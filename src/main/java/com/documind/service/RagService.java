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

    private final OpenAIEmbeddingService embeddingService;
    private final DocumentEmbeddingRepository embeddingRepository;
    private final ChatHistoryRepository chatHistoryRepository;
    private final UserRepository userRepository;
    private final ChatLanguageModel chatModel;

    public RagService(
            OpenAIEmbeddingService embeddingService,
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

        User user = userRepository.findByUsername(username).orElse(null);

        List<String> topChunks;

        try {
            // 🔥 Generate embedding
            var embedding = embeddingService.embed(question);
            float[] vector = embedding.vector();

            System.out.println("VECTOR SIZE: " + vector.length);

            String vectorString = convertToVectorString(vector);

            System.out.println("VECTOR SAMPLE: " +
                    vectorString.substring(0, Math.min(100, vectorString.length())));

            // 🔥 Vector search
            topChunks = embeddingRepository.findTop3SimilarByUser(vectorString);

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error in vector search: " + e.getMessage();
        }

        // 🚨 No data
        if (topChunks == null || topChunks.isEmpty()) {
            return "⚠ Please upload a document first!";
        }

        // 🔥 BUILD CONTEXT
        StringBuilder context = new StringBuilder();

        for (String chunk : topChunks) {
            context.append(chunk).append("\n\n");
        }

        System.out.println("📄 CONTEXT:\n" + context);

        // 🔥 PROMPT
        String prompt =
                """
                You are DocuMind AI.

                Rules:
                - Answer clearly in bullet points
                - Use only the provided context
                - Do not mention sources
                - If not found, say: Not found in document

                Context:
                """ + context +

                        "\nQuestion:\n" + question +
                        "\nAnswer:";

        // 🔥 LLM
        String answer = chatModel.generate(prompt);

        // 🔥 Save history
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

    // 🔥 VECTOR FORMAT
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