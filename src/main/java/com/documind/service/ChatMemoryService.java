package com.documind.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatMemoryService {

    private final List<String> history = new ArrayList<>();

    public void addMessage(String message) {
        history.add(message);
    }

    public String getHistory() {
        return String.join("\n", history);
    }

    public void clear() {
        history.clear();
    }
}