package com.beyond.pochaon.common.service;

import com.beyond.pochaon.common.dtos.SseChatAlarmDto;
import com.beyond.pochaon.common.repository.SseChatAlarmRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Component
public class SseChatAlarmService implements MessageListener {
    private final SseChatAlarmRegistry sseChatAlarmRegistry;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;
    @Autowired
    public SseChatAlarmService(SseChatAlarmRegistry sseChatAlarmRegistry, ObjectMapper objectMapper,@Qualifier("ssePubSubChat") RedisTemplate<String, String> redisTemplate) {
        this.sseChatAlarmRegistry = sseChatAlarmRegistry;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

////    채팅 sendMessage에서 호출 // 안하고 redis로 알람받음
//    public void sendChatAlarm(Long storeId, int senderTable, int receiverTable, String message) {
//
////        받는쪽 알람 off면 안보내고, on일때만 send
//        String alarmKey = "alarm:" + storeId + ":" + receiverTable;
//        String status = redisTemplate.opsForValue().get(alarmKey);
//        if("OFF".equals(status)){
//            return;
//        }
//
//        SseChatAlarmDto dto = SseChatAlarmDto.builder()
//                .storeId(storeId)
//                .senderTable(senderTable)
//                .receiverTable(receiverTable)
//                .message(message)
//                .build();
//
//        // 1. receiver가 SSE 연결 중이면 알람 전송
//        try {
//            String data = objectMapper.writeValueAsString(dto);
//            String receiverKey = storeId + "-" + receiverTable;
//            SseEmitter sseEmitter = sseChatAlarmRegistry.getEmitter(receiverKey);
//
//            if(sseEmitter != null){
//                sseEmitter.send(SseEmitter.event()
//                        .name("chat-alarm")
//                        .data(data));
//            } else {
//        // 2. 연결 안되어있으면 Redis 로 위임
//                redisTemplate.convertAndSend("chatting-channel", data);
//            }
//        } catch (Exception e){
//            e.printStackTrace();
//        }
//    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
//        message : 실질적으로 메세지가 담겨있는 객체
//        pattern : 채널명
        try{
            // ChatService가 보내는 구조 그대로 파싱
            Map<String, Object> raw = objectMapper.readValue(message.getBody(), Map.class);

            Long storeId = Long.valueOf(raw.get("storeId").toString());
//            int receiverTable = (int) raw.get("receiverTableNum"); // ChatService 필드명 사용
            Object receiverRaw = raw.get("receiverTableNum") != null
                    ? raw.get("receiverTableNum")
                    : raw.get("receiverTable");
            int receiverTable = Integer.parseInt(receiverRaw.toString());

            String msg = (String) raw.get("message");

            // 알람 ON/OFF 체크
            String alarmKey = "alarm:" + storeId + ":" + receiverTable;
            String status = redisTemplate.opsForValue().get(alarmKey);
            if ("OFF".equals(status)) return;

            // SSE 전송
            SseChatAlarmDto dto = SseChatAlarmDto.builder()
                    .storeId(storeId)
                    .receiverTable(receiverTable)
                    .message(msg)
                    .build();

            String data = objectMapper.writeValueAsString(dto);
            String receiverKey = storeId + "-" + receiverTable;
            SseEmitter sseEmitter = sseChatAlarmRegistry.getEmitter(receiverKey);

            if(sseEmitter != null){
                sseEmitter.send(SseEmitter.event()
                        .name("chat-alarm")
                        .data(data));    //dto를 넣을지 확인
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

//    알람설정 기본 ON
    public void setToggleAlarmStatus(Long storeId, int tableNum, boolean on){
        String alarmKey = "alarm:" + storeId + ":" + tableNum;
        redisTemplate.opsForValue().set(alarmKey, on ? "ON" : "OFF");
    }
}
