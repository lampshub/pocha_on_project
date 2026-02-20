package com.beyond.pochaon.common.controller;

import com.beyond.pochaon.common.dtos.SmsSendReqDto;
import com.beyond.pochaon.common.dtos.SmsVerifyReqDto;
import com.beyond.pochaon.common.service.SmsAuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/sms")
public class SmsController {
    private final SmsAuthService service;

    public SmsController(SmsAuthService service) {
        this.service = service;
    }

    @PostMapping("/send")
    public void send(@RequestBody SmsSendReqDto dto){
        service.send(dto.getName(), dto.getPhone());
    }

    @PostMapping("/verify")
    public String verify(@RequestBody SmsVerifyReqDto dto){
        return service.verify(
                dto.getName(),
                dto.getPhone(),
                dto.getCode()
        );
    }
}
