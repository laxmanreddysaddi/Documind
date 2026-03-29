package com.documind.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ChatService {

    private final RagService ragService;

    // 🔥 In-memory chat history (per user)
    private final Map<String, List<String>> chatHistory = new HashMap<>();

    public ChatService(RagService ragService) {
        this.ragService = ragService;
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

        // 🔥 STRICT RAG CALL (FIXED)
        String answer = ragService.ask(question, documentId);

        // ================= SAVE CHAT =================
        chatHistory.putIfAbsent(username, new ArrayList<>());

        chatHistory.get(username).add("Q: " + question);
        chatHistory.get(username).add("A: " + answer);

        return answer;
    }

    // =========================
    // ✅ GET HISTORY
    // =========================
    public List<String> getChatHistory(String username) {
        return chatHistory.getOrDefault(username, new ArrayList<>());
    }

    // =========================
    // ✅ CLEAR CHAT
    // =========================
    public void clearChat(String username) {
        chatHistory.remove(username);
    }
}