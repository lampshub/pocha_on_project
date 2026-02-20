package com.beyond.pochaon.common.controller;


import com.beyond.pochaon.common.dtos.EmailRequestDto;
import com.beyond.pochaon.common.dtos.EmailVerifyRequest;
import com.beyond.pochaon.common.service.EmailAuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/email")
public class EmailAuthController {
    private final EmailAuthService emailAuthService;

    public EmailAuthController(EmailAuthService emailAuthService) {
        this.emailAuthService = emailAuthService;
    }

    // 인증코드 발송
    @PostMapping("/send")
    public void send(@RequestBody EmailRequestDto dto){
        emailAuthService.sendCode(dto.getEmail());
    }

    // 인증 확인 (redirect 방식)
    @PostMapping("/verify")
    public void verify(@RequestBody EmailVerifyRequest req){
        emailAuthService.verifyCode(req.getEmail(), req.getCode());
    }
}
