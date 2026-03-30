package com.documind.service;

import com.documind.model.ChatSession;
import com.documind.repository.ChatSessionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatSessionService {

    private final ChatSessionRepository repo;

    public ChatSessionService(ChatSessionRepository repo) {
        this.repo = repo;
    }

    public ChatSession create(String username, Long documentId, String firstQuestion) {

    ChatSession session = new ChatSession();
    session.setUsername(username);
    session.setDocumentId(documentId);
    session.setCreatedAt(LocalDateTime.now());

    // 🔥 AUTO NAME FROM QUESTION
    if (firstQuestion != null && !firstQuestion.isBlank()) {
        String name = firstQuestion.length() > 30
                ? firstQuestion.substring(0, 30) + "..."
                : firstQuestion;
        session.setName(name);
    } else {
        session.setName("New Chat");
    }

    return repo.save(session);
}

    public List<ChatSession> getSessions(String username, Long documentId) {
        return repo.findByUsernameAndDocumentIdOrderByCreatedAtDesc(username, documentId);
    }

    public void rename(Long id, String name) {
        ChatSession session = repo.findById(id).orElseThrow();
        session.setName(name);
        repo.save(session);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}