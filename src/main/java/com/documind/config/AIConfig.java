package com.documind.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.huggingface.HuggingFaceChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return HuggingFaceChatModel.builder()
                .accessToken(System.getenv("HF_TOKEN"))
                .modelId("mistralai/Mistral-7B-Instruct-v0.2") // FREE model
                .build();
    }
}