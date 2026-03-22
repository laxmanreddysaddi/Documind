package com.documind.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

@Service
public class ChatService {

    private final RagService ragService;

    public ChatService(RagService ragService) {
        this.ragService = ragService;
    }

    // Normal chat
    public String ask(String question, String username) {
        return ragService.ask(question, username);
    }

    // Chat history
    public List<String> getChatHistory(String username) {
        return List.of("No chat history yet");
    }

    // Streaming chat
    public Flux<String> streamAnswer(String question, String username) {

        String answer = ragService.ask(question, username);

        return Flux.fromArray(answer.split(" "))
                .delayElements(Duration.ofMillis(40));
    }
}