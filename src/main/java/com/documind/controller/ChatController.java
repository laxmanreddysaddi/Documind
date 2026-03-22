package com.documind.controller;

import com.documind.service.ChatService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public String chat(@RequestParam String question, Authentication authentication) {

        String username = "anonymous";

        if (authentication != null) {
            username = authentication.getName();
        }

        return chatService.ask(question, username);
    }

    @GetMapping("/history")
    public List<String> getChatHistory(Authentication authentication) {

        if (authentication == null) {
            throw new RuntimeException("User not authenticated");
        }

        String username = authentication.getName();

        return chatService.getChatHistory(username);
    }
}