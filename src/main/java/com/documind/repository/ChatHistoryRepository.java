package com.documind.repository;

import com.documind.model.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {

    List<ChatHistory> findTop5ByUserUsernameOrderByTimestampDesc(String username);

}