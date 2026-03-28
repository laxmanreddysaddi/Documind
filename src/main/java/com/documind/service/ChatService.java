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

    // ✅ UPDATED METHOD (IMPORTANT)
    public String ask(String question, String username, Long documentId) {

        // 🔥 Call RAG with documentId
        String answer = ragService.ask(question, documentId);

        // ✅ Save chat history per user
        chatHistory.putIfAbsent(username, new ArrayList<>());

        chatHistory.get(username).add("Q: " + question);
        chatHistory.get(username).add("A: " + answer);

        return answer;
    }

    // ✅ Get chat history
    public List<String> getChatHistory(String username) {
        return chatHistory.getOrDefault(username, new ArrayList<>());
    }

    // ✅ Clear chat history (NEW FEATURE)
    public void clearChat(String username) {
        chatHistory.remove(username);
    }
}