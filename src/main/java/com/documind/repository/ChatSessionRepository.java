package com.documind.repository;

import com.documind.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    List<ChatSession> findByUsernameAndDocumentIdOrderByCreatedAtDesc(
            String username, Long documentId
    );
}