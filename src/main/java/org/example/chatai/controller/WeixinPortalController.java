package org.example.chatai.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.chatai.common.MessageTextEntity;
import org.example.chatai.common.SignatureUtil;
import org.example.chatai.common.XmlUtil;
import org.example.chatai.service.ILoginService;
import org.example.chatai.service.OpenAIService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/v1/weixin/portal")
public class WeixinPortalController {

    @Value("${weixin.config.originalid}")
    private String originalid;

    @Value("${weixin.config.token}")
    private String token;

    @Resource
    private ILoginService loginService;

    @Resource
    private OpenAIService openAIService;

    // 验签接口
    @GetMapping(value = "receive", produces = "text/plain;charset=utf-8")
    public ResponseEntity<String> validate(
            @RequestParam("signature") String signature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestParam("echostr") String echostr) {
        try {
            log.info("微信公众号验签开始 [{}, {}, {}, {}]", signature, timestamp, nonce, echostr);
            if (StringUtils.isAnyBlank(signature, timestamp, nonce, echostr)) {
                log.warn("请求参数非法");
                return ResponseEntity.badRequest().body("请求参数非法");
            }
            boolean check = SignatureUtil.check(token, signature, timestamp, nonce);
            log.info("微信公众号验签结果：{}", check);
            return check
                    ? ResponseEntity.ok(echostr)
                    : ResponseEntity.status(403).body("验签失败");
        } catch (Exception e) {
            log.error("微信公众号验签异常", e);
            return ResponseEntity.status(500).body("服务器内部错误");
        }
    }

    // 消息处理接口
    @PostMapping(
            value = "receive",
            consumes = MediaType.TEXT_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE
    )
    public CompletableFuture<ResponseEntity<String>> handleMessage(
            @RequestBody byte[] requestBytes,
            @RequestParam("signature") String signature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestParam("openid") String openid) {

        try {
            // 验签
            boolean check = SignatureUtil.check(token, signature, timestamp, nonce);
            if (!check) {
                log.warn("验签失败，openid: {}, signature: {}", openid, signature);
                return CompletableFuture.completedFuture(
                        ResponseEntity.status(403).body("验签失败")
                );
            }

            // 解析请求体
            String requestBody = new String(requestBytes, StandardCharsets.UTF_8);
            log.info("收到来自用户 [{}] 的原始请求:\n{}", openid, requestBody);

            // 解析XML
            MessageTextEntity message = XmlUtil.xmlToBean(requestBody, MessageTextEntity.class);
            log.info("解析后的消息对象: {}", message);

            // 消息处理
            switch (message.getMsgType()) {
                case "event":
                    return handleEvent(message, openid)
                            .thenApply(response -> ResponseEntity.ok(response));
                case "text":
                    return handleTextMessage(message, openid)
                            .thenApply(response -> ResponseEntity.ok(response));
                default:
                    log.warn("不支持的消息类型: {}", message.getMsgType());
                    return CompletableFuture.completedFuture(
                            ResponseEntity.ok(buildResponse(openid, "暂不支持该消息类型"))
                    );
            }
        } catch (Exception e) {
            log.error("处理消息时发生异常，openid: {}", openid, e);
            return CompletableFuture.completedFuture(
                    ResponseEntity.status(500)
                            .body(buildErrorResponse(openid, "服务暂时不可用，请稍后重试"))
            );
        }
    }

    // 事件处理逻辑
    private CompletableFuture<String> handleEvent(MessageTextEntity message, String openid) throws IOException {
        log.info("处理事件消息，openid: {}, event: {}", openid, message.getEvent());
        switch (message.getEvent()) {
            case "SCAN":
                loginService.saveLoginState(message.getTicket(), openid);
                return CompletableFuture.completedFuture(
                        buildResponse(openid, "扫码成功！我是AI助手")
                );
            case "subscribe":
                return CompletableFuture.completedFuture(
                        buildResponse(openid, "感谢关注！我是AI助手")
                );
            case "unsubscribe":
                log.info("用户 [{}] 取消关注", openid);
                return CompletableFuture.completedFuture("success"); // 微信要求返回 "success"
            default:
                log.warn("暂不支持的事件类型: {}", message.getEvent());
                return CompletableFuture.completedFuture(
                        buildResponse(openid, "暂不支持此事件类型")
                );
        }
    }

    // 文本消息处理逻辑
    private CompletableFuture<String> handleTextMessage(MessageTextEntity message, String openid) {
        log.info("处理文本消息，openid: {}, content: {}", openid, message.getContent());
        return openAIService.askQuestion(message.getContent())
                .thenApply(response -> {
                    if (StringUtils.isBlank(response)) {
                        log.warn("AI返回空结果，openid: {}", openid);
                        return buildResponse(openid, "抱歉，我暂时无法回答这个问题");
                    }
                    log.info("AI回复用户 [{}]: {}", openid, response);
                    return buildResponse(openid, response);
                })
                .exceptionally(e -> {
                    log.error("调用OpenAI服务失败，openid: {}", openid, e);
                    return buildErrorResponse(openid, "AI服务暂时不可用");
                });
    }

    // 构建成功响应
    private String buildResponse(String openid, String content) {
        return buildMessage(openid, content);
    }

    // 构建错误响应
    private String buildErrorResponse(String openid, String errorMsg) {
        return buildMessage(openid, errorMsg);
    }

    private String buildMessage(String openid, String content) {
        MessageTextEntity res = new MessageTextEntity();
        res.setFromUserName(originalid);
        res.setToUserName(openid);
        res.setCreateTime(String.valueOf(System.currentTimeMillis() / 1000L));
        res.setMsgType("text");
        res.setContent(content);

        String xml = XmlUtil.beanToXml(res);
        log.info("生成响应XML:\n{}", xml);
        return xml;
    }
}
