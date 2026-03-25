package com.documind.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {

  @Bean
public ChatLanguageModel chatLanguageModel() {
    return OpenAiChatModel.builder()
            .apiKey(System.getenv("OPENROUTER_API_KEY"))
            .baseUrl("https://openrouter.ai/api/v1")
           .modelName("meta-llama/llama-3-8b-instruct:free") // ✅ FINAL FIX
            .build();
}
}