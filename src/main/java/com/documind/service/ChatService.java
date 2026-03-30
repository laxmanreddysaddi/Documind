package com.documind.service;

import com.documind.model.ChatHistory;
import com.documind.model.User;
import com.documind.repository.ChatHistoryRepository;
import com.documind.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService {

    private final RagService ragService;
    private final ChatHistoryRepository chatRepo;
    private final UserRepository userRepo;

    public ChatService(
            RagService ragService,
            ChatHistoryRepository chatRepo,
            UserRepository userRepo
    ) {
        this.ragService = ragService;
        this.chatRepo = chatRepo;
        this.userRepo = userRepo;
    }

    // =========================
    // ✅ MAIN CHAT METHOD
    // =========================
    public String ask(String question, String username, Long documentId) {

        // ❌ Validate inputs
        if (question == null || question.trim().isEmpty()) {
            return "⚠ Please enter a question";
        }

        if (documentId == null) {
            return "⚠ Select document first";
        }

        // 🔥 CALL RAG
        String answer = ragService.ask(question, documentId);

        // ================= SAVE TO DATABASE =================
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatHistory chat = new ChatHistory();
        chat.setQuestion(question);
        chat.setAnswer(answer);
        chat.setTimestamp(LocalDateTime.now());
        chat.setUser(user);

        chatRepo.save(chat);

        return answer;
    }

    // =========================
    // ✅ GET HISTORY
    // =========================
    public List<ChatHistory> getChatHistory(String username) {
        return chatRepo.findByUserUsernameOrderByTimestampAsc(username);
    }

    // =========================
    // ✅ CLEAR CHAT
    // =========================
    public void clearChat(String username) {
        chatRepo.deleteByUserUsername(username);
    }
}