package com.documind.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    private final RagService ragService;

    public ChatService(RagService ragService) {
        this.ragService = ragService;
    }

    // ✅ Normal chat
    public String ask(String question, String username) {
        return ragService.ask(question, username);
    }

    // ✅ Chat history (dummy for now)
    public List<String> getChatHistory(String username) {
        return List.of("No chat history yet");
    }
}