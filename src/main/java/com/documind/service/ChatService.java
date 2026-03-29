package com.documind.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ChatService {

    private final RagService ragService;

    // 🔥 In-memory chat storage (per user)
    private final Map<String, List<String>> chatHistory = new HashMap<>();

    public ChatService(RagService ragService) {
        this.ragService = ragService;
    }

    // =========================
    // ✅ ASK QUESTION (FIXED)
    // =========================
    public String ask(String question, String username, Long documentId) {

        if (documentId == null) {
            return "⚠ Please select a document";
        }

        // ✅ FIXED CALL
        String answer = ragService.ask(question, username, documentId);

        // =========================
        // 💾 SAVE CHAT PER USER
        // =========================
        chatHistory.putIfAbsent(username, new ArrayList<>());

        chatHistory.get(username).add("Q: " + question);
        chatHistory.get(username).add("A: " + answer);

        return answer;
    }

    // =========================
    // 📜 GET CHAT HISTORY
    // =========================
    public List<String> getChatHistory(String username) {
        return chatHistory.getOrDefault(username, new ArrayList<>());
    }

    // =========================
    // 🧹 CLEAR CHAT
    // =========================
    public void clearChat(String username) {
        chatHistory.remove(username);
    }
}