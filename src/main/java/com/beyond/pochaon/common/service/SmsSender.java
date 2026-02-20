package com.beyond.pochaon.common.service;


import com.beyond.pochaon.common.config.CoolSmsConfig;
import lombok.extern.slf4j.Slf4j;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.exception.NurigoEmptyResponseException;
import net.nurigo.sdk.message.exception.NurigoMessageNotReceivedException;
import net.nurigo.sdk.message.exception.NurigoUnknownException;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsSender {

    private final CoolSmsConfig config;

    public SmsSender(CoolSmsConfig config) {
        this.config = config;
    }

    public void send(String to, String code) {

        // 전화번호 형식 검증 (하이픈 제거)
        String cleanPhone = to.replaceAll("-", "");

        log.info("=== SMS 발송 시도 ===");
        log.info("발신번호: " + config.getFromNumber());
        log.info("수신번호: " + cleanPhone);
        log.info ("API Key: " + config.getApiKey().substring(0, 5) + "***");

        DefaultMessageService messageService =
                NurigoApp.INSTANCE.initialize(
                        config.getApiKey(),
                        config.getApiSecret(),
                        "https://api.solapi.com"
                );

        Message message = new Message();
        message.setFrom(config.getFromNumber());
        message.setTo(cleanPhone);  // 정제된 번호 사용
        message.setText("[인증번호] " + code);

        try {
            messageService.send(message);
            System.out.println("SMS 발송 성공!");
        } catch (NurigoMessageNotReceivedException e) {
            System.err.println("발송 실패: " + e.getMessage());
            throw new RuntimeException("SMS 발송 접수 실패: " + e.getMessage(), e);
        } catch (NurigoEmptyResponseException e) {
            System.err.println("응답 없음: " + e.getMessage());
            throw new RuntimeException("SMS 서버 응답 없음: " + e.getMessage(), e);
        } catch (NurigoUnknownException e) {
            System.err.println("알 수 없는 오류: " + e.getMessage());
            throw new RuntimeException("SMS 발송 중 오류: " + e.getMessage(), e);
        }
    }
}
