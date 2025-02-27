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
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OpenAIService {

    private final OpenAIConfig openAIConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate; // 添加 Redis 模板

    @Autowired
    public OpenAIService(OpenAIConfig openAIConfig, RestTemplate restTemplate, ObjectMapper objectMapper,
                         StringRedisTemplate redisTemplate) {
        this.openAIConfig = openAIConfig;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate; // 初始化 Redis 模板
    }

    @Async
    public CompletableFuture<String> askQuestion(String userId, String question) {
        try {
            log.info("Received question from user [{}]: {}", userId, question);

            // 构建 Redis 缓存键
            String cacheKey = "chat:context:" + userId;

            // 从 Redis 获取历史上下文
            String previousContext = redisTemplate.opsForValue().get(cacheKey);
            if (previousContext == null) {
                log.info("No previous context found for user [{}], initializing new context.", userId);
                previousContext = ""; // 如果没有历史上下文，初始化为空字符串
            } else {
                log.info("Retrieved previous context for user [{}]: {}", userId, previousContext);
            }

            // 拼接新的上下文
            String context = previousContext + "\nUser: " + question;

            // 打印上下文内容
            log.info("Updated context for user [{}]: {}", userId, context);

            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAIConfig.getKey());

            // 构建请求体
            OpenAIRequest request = new OpenAIRequest();
            request.setModel("gpt-3.5-turbo");
            OpenAIRequest.Message message = new OpenAIRequest.Message("user", context);
            request.setMessages(Collections.singletonList(message));

            // 打印请求体日志，确保上下文正确
            log.debug("Constructed OpenAI request for user [{}]: {}", userId, objectMapper.writeValueAsString(request));

            // 发送请求
            HttpEntity<OpenAIRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<OpenAIResponse> response = restTemplate.exchange(
                    openAIConfig.getEndpoint(),
                    HttpMethod.POST,
                    entity,
                    OpenAIResponse.class
            );

            // 处理 OpenAI 返回结果
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null
                    && !response.getBody().getChoices().isEmpty()) {
                String answer = response.getBody().getChoices().get(0).getMessage().getContent();
                log.info("Successfully received response from OpenAI for user [{}]: {}", userId, answer);

                // 将新的对话内容追加到上下文
                String updatedContext = context + "\nAI: " + answer;

                // 写入 Redis 中的上下文
                redisTemplate.opsForValue().set(cacheKey, updatedContext, 30, TimeUnit.MINUTES); // 设置过期时间为 30 分钟
                log.info("Successfully updated Redis context for user [{}]: {}", userId, updatedContext);

                return CompletableFuture.completedFuture(answer);
            } else {
                log.warn("Received unexpected response from OpenAI for user [{}]: status={}, body={}",
                        userId, response.getStatusCode(), response.getBody());
                return CompletableFuture.completedFuture("抱歉，我现在无法回答，请稍后再试");
            }
        } catch (Exception e) {
            log.error("Error while calling OpenAI API for user [{}]", userId, e);
            return CompletableFuture.completedFuture("服务暂时不可用，请稍后重试");
        }
    }
}
