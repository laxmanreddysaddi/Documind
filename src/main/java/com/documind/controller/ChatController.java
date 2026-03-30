package com.documind.controller;

import com.documind.model.ChatHistory;
import com.documind.model.ChatSession;
import com.documind.service.ChatService;
import com.documind.service.ChatSessionService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin("*")
public class ChatController {

    private final ChatService chatService;
    private final ChatSessionService sessionService;

    // ✅ CONSTRUCTOR INJECTION (VERY IMPORTANT)
    public ChatController(ChatService chatService,
                          ChatSessionService sessionService) {
        this.chatService = chatService;
        this.sessionService = sessionService;
    }

    // ================= SESSION =================

   @PostMapping("/session/create")
public ResponseEntity<?> createSession(
        @RequestParam String username,
        @RequestParam Long documentId,
        @RequestParam(required = false) String question
) {
    return ResponseEntity.ok(
        sessionService.create(username, documentId, question)
    );
}

    @GetMapping("/sessions")
    public ResponseEntity<?> getSessions(
            @RequestParam String username,
            @RequestParam Long documentId
    ) {
        return ResponseEntity.ok(
                sessionService.getSessions(username, documentId)
        );
    }

    @PutMapping("/session/rename")
    public ResponseEntity<?> rename(
            @RequestParam Long sessionId,
            @RequestParam String name
    ) {
        sessionService.rename(sessionId, name);
        return ResponseEntity.ok("Renamed");
    }

    @DeleteMapping("/session/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        sessionService.delete(id);
        return ResponseEntity.ok("Deleted");
    }

    // ================= CHAT =================

    @PostMapping("/ask")
    public ResponseEntity<?> ask(
            @RequestParam String question,
            @RequestParam Long sessionId,
            @RequestParam Long documentId
    ) {
        return ResponseEntity.ok(
                chatService.ask(question, sessionId, documentId)
        );
    }

    @GetMapping("/history")
    public ResponseEntity<?> history(
            @RequestParam Long sessionId
    ) {
        return ResponseEntity.ok(
                chatService.getHistory(sessionId)
        );
    }
}