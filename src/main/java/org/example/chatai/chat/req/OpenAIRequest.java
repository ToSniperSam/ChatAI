package org.example.chatai.chat.req;

import java.util.List;

public class OpenAIRequest {
    private String model;
    private List<Message> messages;

    // Getters and Setters
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public static class Message {
        private String role;
        private String content;

        // 全参构造函数
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        // 默认构造函数（必须保留）
        public Message() {}

        // Getters and Setters
        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
