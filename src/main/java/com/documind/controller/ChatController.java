package com.documind.controller;

import com.documind.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin("*")
public class ChatController {

    private final ChatService chatService; // ✅ FIX

    // ✅ CONSTRUCTOR FIX
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // =========================
    // ✅ ASK QUESTION
    // =========================
    @PostMapping("/ask")
    public ResponseEntity<?> ask(
            @RequestParam String question,
            @RequestParam String username,
            @RequestParam Long documentId
    ) {
        return ResponseEntity.ok(
                chatService.ask(question, username, documentId) // ✅ FIX
        );
    }

    // =========================
    // ✅ GET CHAT HISTORY
    // =========================
    @GetMapping("/history")
    public ResponseEntity<?> history(
            @RequestParam String username
    ) {
        return ResponseEntity.ok(
                chatService.getChatHistory(username)
        );
    }

    // =========================
    // ✅ CLEAR CHAT
    // =========================
    @DeleteMapping("/clear")
    public ResponseEntity<?> clear(
            @RequestParam String username
    ) {
        chatService.clearChat(username);
        return ResponseEntity.ok("✅ Chat cleared");
    }
}