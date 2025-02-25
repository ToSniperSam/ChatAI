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
    public CompletableFuture<String> testChat(@RequestBody String message) {
        log.info("Received test request with message: {}", message);
        return openAIService.askQuestion(message);
    }
}
