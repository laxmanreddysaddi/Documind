package com.documind.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ChatHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    private LocalDateTime timestamp;

    // 🔥 NEW FIELD (VERY IMPORTANT)
    private Long sessionId;

    // ===== GETTERS & SETTERS =====

    public Long getId() { return id; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    // 🔥 SESSION ID
    public Long getSessionId() { return sessionId; }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }
}