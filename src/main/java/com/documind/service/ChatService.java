package com.documind.service;

import com.documind.model.ChatHistory;
import com.documind.repository.ChatHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService {

    private final RagService ragService;
    private final ChatHistoryRepository chatRepo;

    public ChatService(RagService ragService,
                       ChatHistoryRepository chatRepo) {
        this.ragService = ragService;
        this.chatRepo = chatRepo;
    }

    // ✅ ASK WITH SESSION
    public String ask(String question, Long sessionId, Long documentId) {

        String answer = ragService.ask(question, documentId);

        ChatHistory chat = new ChatHistory();
        chat.setQuestion(question);
        chat.setAnswer(answer);
        chat.setTimestamp(LocalDateTime.now());
        chat.setSessionId(sessionId);

        chatRepo.save(chat);

        return answer;
    }

    // ✅ GET HISTORY
    public List<ChatHistory> getHistory(Long sessionId) {
        return chatRepo.findBySessionIdOrderByTimestampAsc(sessionId);
    }
}