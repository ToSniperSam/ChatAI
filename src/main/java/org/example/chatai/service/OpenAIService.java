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
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OpenAIService {

    private final OpenAIConfig openAIConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final ChatRecordRepository chatRecordRepository; // 引入数据库存储仓库

    // 设定最大缓存上下文长度（字符数）
    private static final int MAX_CONTEXT_LENGTH = 3000;

    @Autowired
    public OpenAIService(OpenAIConfig openAIConfig,
                         RestTemplate restTemplate,
                         ObjectMapper objectMapper,
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

            // 1. 从 Redis 获取之前的上下文（字符串）
            String cacheKey = "chat:context:" + userId;
            String previousContext = redisTemplate.opsForValue().get(cacheKey);

            // 若没有历史上下文，则初始化为空
            if (previousContext == null) {
                log.info("No previous context found for user [{}], initializing empty context.", userId);
                previousContext = "";
            } else {
                log.info("Retrieved previous context for user [{}]: {}", userId, previousContext);
            }

            // 2. 将新问题拼接到上下文
            String context = "User: " + question + "\n" + (previousContext == null ? "" : previousContext);

            // 如果上下文过长，则截断前面部分（保持最新的内容）
            if (context.length() > MAX_CONTEXT_LENGTH) {
                context = context.substring(context.length() - MAX_CONTEXT_LENGTH);
            }

            log.info("Updated context for user [{}]: {}", userId, context);

            // 3. 构建请求体并调用 OpenAI 接口
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAIConfig.getKey());

            OpenAIRequest request = new OpenAIRequest();
            request.setModel("gpt-3.5-turbo");
            OpenAIRequest.Message message = new OpenAIRequest.Message("user", context);
            request.setMessages(java.util.Collections.singletonList(message));

            log.debug("Constructed OpenAI request for user [{}]: {}", userId, objectMapper.writeValueAsString(request));

            HttpEntity<OpenAIRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<OpenAIResponse> response = restTemplate.exchange(
                    openAIConfig.getEndpoint(),
                    HttpMethod.POST,
                    entity,
                    OpenAIResponse.class
            );

            // 4. 处理响应结果
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null
                    && !response.getBody().getChoices().isEmpty()) {

                String answer = response.getBody().getChoices().get(0).getMessage().getContent();
                log.info("Successfully received response from OpenAI for user [{}]: {}", userId, answer);

                // 5. 将 AI 的回答拼接回上下文
                String updatedContext = "AI: " + answer + "\n" + context;

                // 若拼接后仍过长，则同样截断
                if (updatedContext.length() > MAX_CONTEXT_LENGTH) {
                    updatedContext = updatedContext.substring(0, MAX_CONTEXT_LENGTH);
                }

                // 将新的上下文存入 Redis，设置过期时间
                redisTemplate.opsForValue().set(cacheKey, updatedContext, 30, TimeUnit.MINUTES);
                log.info("Successfully updated Redis context for user [{}].", userId);

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
