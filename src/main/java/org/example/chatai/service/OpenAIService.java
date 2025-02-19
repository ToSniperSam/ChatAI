package org.example.chatai.service;

import lombok.extern.slf4j.Slf4j;
import org.example.chatai.chat.config.OpenAIConfig;
import org.example.chatai.chat.req.OpenAIRequest;
import org.example.chatai.chat.res.OpenAIResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;


@Slf4j
@Service
public class OpenAIService {

    private final OpenAIConfig openAIConfig;
    private final RestTemplate restTemplate;

    @Autowired
    public OpenAIService(OpenAIConfig openAIConfig) {
        this.openAIConfig = openAIConfig;
        this.restTemplate = new RestTemplate();
    }

    public String askQuestion(String question) {
        try {
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + openAIConfig.getKey());

            // 构建请求体
            OpenAIRequest request = new OpenAIRequest();
            request.setModel("gpt-3.5-turbo"); // 使用 GPT-3.5 模型
            OpenAIRequest.Message message = new OpenAIRequest.Message();
            message.setRole("user");
            message.setContent(question);
            request.setMessages(Collections.singletonList(message));

            // 发送请求
            HttpEntity<OpenAIRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<OpenAIResponse> response = restTemplate.exchange(
                    openAIConfig.getEndpoint(),
                    HttpMethod.POST,
                    entity,
                    OpenAIResponse.class
            );

            // 处理响应
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody().getChoices().get(0).getMessage().getContent();
            } else {
                return "Error: Unable to get response from OpenAI";
            }
        } catch (Exception e) {
            log.error("Error communicating with OpenAI: ", e);
            return "Error: Communication failed with OpenAI";
        }
    }
}
