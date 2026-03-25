package com.documind.controller;

import com.documind.model.ChatHistory;
import com.documind.repository.ChatHistoryRepository;
import com.documind.service.RagService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class ChatController {

    private final RagService ragService;
    private final ChatHistoryRepository chatHistoryRepository;

    public ChatController(RagService ragService,
                          ChatHistoryRepository chatHistoryRepository) {
        this.ragService = ragService;
        this.chatHistoryRepository = chatHistoryRepository;
    }

    // ✅ Chat API
    @GetMapping("/chat")
    public String chat(@RequestParam String question,
                       @RequestParam String username) {
        return ragService.ask(question, username);
    }

    // ✅ Get chat history
    @GetMapping("/history/{username}")
    public List<ChatHistory> getHistory(@PathVariable String username) {
        return chatHistoryRepository.findByUserUsername(username);
    }

    // ✅ Clear chat history
    @DeleteMapping("/history/{username}")
    public void clearHistory(@PathVariable String username) {
        chatHistoryRepository.deleteByUserUsername(username);
    }
}