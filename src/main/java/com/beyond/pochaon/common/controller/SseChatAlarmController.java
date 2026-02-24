package com.beyond.pochaon.common.controller;


import com.beyond.pochaon.common.auth.JwtTokenProvider;
import com.beyond.pochaon.common.repository.SseChatAlarmRegistry;
import com.beyond.pochaon.common.service.SseChatAlarmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/sseChat")
public class SseChatAlarmController {

    private final SseChatAlarmRegistry sseChatAlarmRegistry;
    private final JwtTokenProvider jwtTokenProvider;
    private final SseChatAlarmService sseChatAlarmService;
    @Autowired
    public SseChatAlarmController(SseChatAlarmRegistry sseChatAlarmRegistry, JwtTokenProvider jwtTokenProvider, SseChatAlarmService sseChatAlarmService) {
        this.sseChatAlarmRegistry = sseChatAlarmRegistry;
        this.jwtTokenProvider = jwtTokenProvider;
        this.sseChatAlarmService = sseChatAlarmService;
    }

// 보내는 tableNum, storeId 로 emitter 객체 만듬
    @GetMapping("/connect")     //(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE) => 보안filter 할때 문제있으면 이걸로 바꿔보기
    public SseEmitter connect(@RequestHeader("Authorization") String authorization) throws IOException {
        String token = authorization.replace("Bearer ", "");
        Long storeId = jwtTokenProvider.getStoreId(token);
        int senderTable = jwtTokenProvider.getTableNum(token);
        String emitterKey = storeId + "-" + senderTable;
        SseEmitter sseEmitter = new SseEmitter(60 * 60 * 100L); //1시간
        sseChatAlarmRegistry.addSseEmitter(emitterKey, sseEmitter);
        sseEmitter.send(SseEmitter.event().name("connect").data("채팅 알림 SSE연결 완료"));
        return sseEmitter;
    }

    @GetMapping("/disconnect")
    public void disconnect(@RequestHeader("Authorization") String authorization) throws IOException {
        String token = authorization.replace("Bearer", "");
        Long storeId = jwtTokenProvider.getStoreId(token);
        int senderTable = jwtTokenProvider.getTableNum(token);
        System.out.println("disconnect start");
        sseChatAlarmRegistry.removeEmitter(storeId + "-" + senderTable);
    }

//    알람 on/off 설정
    @PostMapping("/alarm/toggle")
    public void toggleAlarm(@RequestHeader("Authorization") String authorization, @RequestParam boolean on){
        String token = authorization.replace("Bearer ", "");
        Long storeId = jwtTokenProvider.getStoreId(token);
        int tableNum = jwtTokenProvider.getTableNum(token);
        sseChatAlarmService.setToggleAlarmStatus(storeId, tableNum, on);
    }

////테스트용
//    @GetMapping("/test")
//    public String testAlarm(
//            @RequestParam Long storeId,
//            @RequestParam int senderTable,
//            @RequestParam int receiverTable,
//            @RequestParam String message
//    ) {
//        sseChatAlarmService.sendChatAlarm(storeId, senderTable, receiverTable, message);
//        return "알람 전송 완료";
//    }

}