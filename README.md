# ChatAI  

A Spring Boot–based AI assistant for WeChat Official Accounts, integrating OpenAI GPT for multi-turn conversations and built for high concurrency and low latency. Redis stores dialogue context, and Docker deployment ensures easy scalability and maintenance.  

<img src="https://github.com/ToSniperSam/ChatAI/blob/master/src/main/java/org/example/docs/dev-ops/demo.png" alt="demo" style="zoom: 50%;" />

---

## Table of Contents  

1. [Description](#description)  
2. [Features](#features)  
3. [Tech Stack](#tech-stack)  
4. [Demo](#demo)  
5. [Quick Start](#quick-start)  
6. [Configuration](#configuration)  
7. [Contact](#contact)  

---

## Description  

This project connects a WeChat Official Account to an AI backend (OpenAI GPT) to enable multi-turn Q&A and context-aware dialogue. By leveraging asynchronous methods (`CompletableFuture` + `@Async`) and Docker-based deployment, it effectively handles high concurrency. Redis is used to store conversation context while preventing memory overuse. In testing, average response time dropped from 8.5 seconds to about 3.2 seconds.  

---

## Features  

- **OpenAI Integration**: Multi-turn conversation and natural language understanding.  
- **Async Processing**: Improved concurrency with async methods.  
- **Redis Context**: Stores user dialogue context (e.g., 30-minute TTL) to avoid memory inflation.  
- **Docker Deployment**: Simplifies setup and scaling.  
- **WeChat Official Account**: Receives messages, sends automated or AI-generated replies.  

---

## Tech Stack  

- **Backend**: Java 8+ / Spring Boot  
- **Database**: MySQL  
- **Cache**: Redis  
- **Deployment**: Docker  
- **AI**: OpenAI GPT Models  

---

## Demo  

Below are some screenshots illustrating the system:  

1. **MySQL Database (chat records)**  
   <img src="https://github.com/ToSniperSam/ChatAI/blob/master/src/main/java/org/example/docs/dev-ops/php.png" alt="php" style="zoom:50%;" />
2. **Container Logs (showing user input, AI responses, and context updates)**  
   <img src="https://github.com/ToSniperSam/ChatAI/blob/master/src/main/java/org/example/docs/dev-ops/log.png" alt="log" style="zoom:50%;" />

---

## Quick Start  

1. **Clone the Repository**  

   ```bash  
   git clone https://github.com/YourUsername/chatai.git  
   cd chatai
   ```

2. **Configure the Application**

   - Update `application.yml` or `application.properties` with MySQL, Redis, and OpenAI credentials.

3. **Build and Run**

   ```
   mvn clean install  
   mvn spring-boot:run  
   ```

   The service typically runs on http://localhost:8080/

4. **Docker (Optional)**

   ```
   docker build -t chatai:latest .  
   docker run -d -p 8080:8080 chatai:latest  
   ```

5. **WeChat Setup**

   - In the WeChat Official Account platform, configure the server URL and token to point to your deployed service address.

------

## Configuration

Sample `application.yml`:

```yml
spring:  
  datasource:  
    url: jdbc:mysql://localhost:3306/chat  
    username: root  
    password: 123456  
  redis:  
    host: localhost  
    port: 6379  

openai:  
  apiKey: "YOUR_OPENAI_API_KEY"  
```

Adjust these as needed for production (e.g., environment variables, other modes).

------

## Contact

- **Maintainer**: [Your Name](mailto:your.email@example.com)
- **Issues & Suggestions**: Submit via [GitHub Issues](https://github.com/YourUsername/chatai/issues).

------

Thank you for using ChatAI! Feel free to contribute or share feedback.
