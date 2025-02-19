package org.example.chatai.res;

import java.util.List;

public class OpenAIResponse {
    private List<Choice> choices;

    // Getters and Setters
    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    @Override
    public String toString() {
        // 返回响应的主要内容，确保打印有用信息
        return "OpenAIResponse{" +
                "choices=" + choices +
                '}';
    }

    public static class Choice {
        private Message message;

        // Getters and Setters
        public Message getMessage() {
            return message;
        }



        public void setMessage(Message message) {
            this.message = message;
        }

        public static class Message {
            private String role;
            private String content;

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
}
