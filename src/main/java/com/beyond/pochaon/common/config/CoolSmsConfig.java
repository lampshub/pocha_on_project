package com.beyond.pochaon.common.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Slf4j
public class CoolSmsConfig {

    @Value("${coolsms.api-key}")
    private String apiKey;

    @Value("${coolsms.api-secret}")
    private String apiSecret;

    @Value("${coolsms.from-number}")
    private String fromNumber;

    @PostConstruct
    public void init() {
        log.info("=== CoolSmsConfig 로드 ===");
        log.info("API Key: " + (apiKey != null ? apiKey.substring(0, 5) + "***" : "NULL"));
        log.info("발신번호: " + fromNumber);
        log.info("=======================");
    }
}
