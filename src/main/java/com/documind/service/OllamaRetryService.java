package com.documind.service;

import org.springframework.stereotype.Service;

@Service
public class OllamaRetryService {

    public <T> T executeWithRetry(RetryableOperation<T> operation) {

        int retries = 3;
        long delay = 2000;

        for (int i = 1; i <= retries; i++) {
            try {
                return operation.execute();
            } catch (Exception e) {

                System.out.println("⚠️ Ollama retry " + i + " failed: " + e.getMessage());

                if (i == retries) {
                    throw new RuntimeException("Ollama unavailable after retries");
                }

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {}
            }
        }

        throw new RuntimeException("Retry failed");
    }

    public interface RetryableOperation<T> {
        T execute();
    }
}