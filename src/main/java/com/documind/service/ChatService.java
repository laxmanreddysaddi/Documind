package com.documind.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ChatService {

    private final RagService ragService;

    private final Map<String, List<String>> chatHistory = new HashMap<>();

    public ChatService(RagService ragService) {
        this.ragService = ragService;
    }

    public String ask(String question, String username, Long documentId) {

        if (documentId == null) {
            return "⚠ Select document first";
        }

        String answer = ragService.ask(question, username, documentId);

        chatHistory.putIfAbsent(username, new ArrayList<>());

        chatHistory.get(username).add("Q: " + question);
        chatHistory.get(username).add("A: " + answer);

        return answer;
    }

    public List<String> getChatHistory(String username) {
        return chatHistory.getOrDefault(username, new ArrayList<>());
    }

    public void clearChat(String username) {
        chatHistory.remove(username);
    }
}