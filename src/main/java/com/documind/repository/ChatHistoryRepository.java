package com.documind.repository;

import com.documind.model.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {

    // ✅ Get user-specific history
    List<ChatHistory> findByUserUsername(String username);

    // ✅ Delete user history
    void deleteByUserUsername(String username);
}