package com.documind.repository;

import com.documind.model.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {

    // ✅ FIX THIS LINE
    List<ChatHistory> findByUserUsernameOrderByTimestampAsc(String username);
    List<ChatHistory> findBySessionIdOrderByTimestampAsc(Long sessionId);

    // ✅ For clear chat
    void deleteByUserUsername(String username);
}