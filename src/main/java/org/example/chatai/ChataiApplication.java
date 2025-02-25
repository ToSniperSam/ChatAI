package org.example.chatai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class ChataiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChataiApplication.class, args);
    }

}
