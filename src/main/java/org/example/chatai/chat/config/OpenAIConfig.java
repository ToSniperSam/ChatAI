package org.example.chatai.chat.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAIConfig {

    @Value("${openai.api.key}")
    private String apiKey;

    // 自定义的 OpenAI API 端点
    @Getter
    @Value("${openai.api.endpoint}")
    private String endpoint;

    public String getKey() {
        return apiKey;
    }

}
