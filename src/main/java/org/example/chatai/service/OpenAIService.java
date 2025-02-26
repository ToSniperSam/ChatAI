package org.example.chatai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.chatai.chat.config.OpenAIConfig;
import org.example.chatai.chat.req.OpenAIRequest;
import org.example.chatai.chat.res.OpenAIResponse;
import org.example.chatai.common.ChatRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.Date;

@Slf4j
@Service
public class OpenAIService {

    private final OpenAIConfig openAIConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate; // 引入 Redis 模板
    private final ChatRecordRepository chatRecordRepository; // 引入数据库存储仓库

    @Autowired
    public OpenAIService(OpenAIConfig openAIConfig, RestTemplate restTemplate, ObjectMapper objectMapper,
                         StringRedisTemplate redisTemplate, ChatRecordRepository chatRecordRepository) {
        this.openAIConfig = openAIConfig;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.chatRecordRepository = chatRecordRepository;
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
//            request.setModel("claude-3-haiku-20240307");
            OpenAIRequest.Message message = new OpenAIRequest.Message("user", context);
            request.setMessages(Collections.singletonList(message));

            // 打印请求体日志，确保上下文正确
            log.info("Constructed OpenAI request for user [{}]: {}", userId, objectMapper.writeValueAsString(request));

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

                // **添加日志：写入 Redis 前**
                log.info("Writing to Redis: key={}, value={}", cacheKey, updatedContext);

                // 更新 Redis 中的上下文
                redisTemplate.opsForValue().set(cacheKey, updatedContext, 30, TimeUnit.MINUTES); // 设置过期时间为 30 分钟

                // **添加日志：写入 Redis 后**
                log.info("Successfully wrote to Redis for user [{}]: {}", userId, updatedContext);

                // **保存对话记录到数据库**
                saveChatRecord(userId, question, answer);

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

    /**
     * 保存对话记录到数据库
     */
    private void saveChatRecord(String userId, String question, String answer) {
        try {

            // 去掉换行符
            String formattedText = answer.replace("\n", "").replace("\r", "");
            // 如果以 "AI:" 开头，移除前缀
            if (formattedText.startsWith("AI:")) {
                formattedText = formattedText.substring(3).trim();
            }
            ChatRecord record = new ChatRecord();
            record.setUserId(userId);
            record.setQuestion(question);
            record.setAnswer(formattedText);
            record.setCreatedAt(new Date());

            chatRecordRepository.save(record);
            log.info("Successfully saved chat record for user [{}] to database.", userId);
        } catch (Exception e) {
            log.error("Failed to save chat record for user [{}] to database.", userId, e);
        }
    }
}
