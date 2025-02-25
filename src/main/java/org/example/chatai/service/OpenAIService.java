package org.example.chatai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.chatai.chat.config.OpenAIConfig;
import org.example.chatai.chat.req.OpenAIRequest;
import org.example.chatai.chat.res.OpenAIResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class OpenAIService {

    private final OpenAIConfig openAIConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public OpenAIService(OpenAIConfig openAIConfig, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.openAIConfig = openAIConfig;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Async
    public CompletableFuture<String> askQuestion(String question) {
        try {
            log.info("Preparing to send request to OpenAI, question length: {}", question.length());

            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAIConfig.getKey());

            // 构建请求体
            OpenAIRequest request = new OpenAIRequest();
            request.setModel("gpt-3.5-turbo");
            OpenAIRequest.Message message = new OpenAIRequest.Message("user", question);
            request.setMessages(Collections.singletonList(message));

            // 记录请求详情（注意不要记录完整的API key）
            log.debug("Request to: {}", openAIConfig.getEndpoint());
            log.debug("Request body: {}", objectMapper.writeValueAsString(request));

            // 发送请求
            HttpEntity<OpenAIRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<OpenAIResponse> response = restTemplate.exchange(
                    openAIConfig.getEndpoint(),
                    HttpMethod.POST,
                    entity,
                    OpenAIResponse.class
            );

            // 处理响应
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null
                    && !response.getBody().getChoices().isEmpty()) {
                String answer = response.getBody().getChoices().get(0).getMessage().getContent();
                log.info("Successfully received response from OpenAI, answer length: {}", answer.length());
                return CompletableFuture.completedFuture(answer);
            } else {
                log.warn("Received unexpected response: status={}, body={}",
                        response.getStatusCode(), response.getBody());
                return CompletableFuture.completedFuture("抱歉，我现在无法回答，请稍后再试");
            }
        } catch (Exception e) {
            log.error("Error while calling OpenAI API", e);
            return CompletableFuture.completedFuture("服务暂时不可用，请稍后重试");
        }
    }
}
