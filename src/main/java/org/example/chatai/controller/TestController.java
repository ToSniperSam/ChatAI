package org.example.chatai.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.chatai.service.OpenAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
@Slf4j
@RestController
@RequestMapping("/api/test")
public class TestController {

    private final OpenAIService openAIService;

    @Autowired
    public TestController(OpenAIService openAIService) {
        this.openAIService = openAIService;
    }

    @PostMapping("/chat")
    public CompletableFuture<String> testChat(
            @RequestParam("openid") String openid, // 添加 openid 参数
            @RequestBody String message) {
        log.info("Received test request with openid: {}, message: {}", openid, message);
        return openAIService.askQuestion(openid, message); // 传递 openid 和消息
    }
}
