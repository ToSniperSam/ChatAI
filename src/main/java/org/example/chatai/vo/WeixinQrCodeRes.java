package org.example.chatai.vo;

import lombok.Data;

@Data
public class WeixinQrCodeRes {
    private String ticket;
    private Long expire_seconds;
    private String url;
}
