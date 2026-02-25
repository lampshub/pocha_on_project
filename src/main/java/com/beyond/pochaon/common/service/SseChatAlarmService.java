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
    public SseChatAlarmService(SseChatAlarmRegistry sseChatAlarmRegistry,
                               ObjectMapper objectMapper,
                               @Qualifier("ssePubSubChat") RedisTemplate<String, String> redisTemplate) {
        this.sseChatAlarmRegistry = sseChatAlarmRegistry;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            Map<String, Object> raw = objectMapper.readValue(message.getBody(), Map.class);

            Long storeId = Long.valueOf(raw.get("storeId").toString());

            // receiverTableNum 파싱 (필드명 방어)
            Object receiverRaw = raw.get("receiverTableNum") != null
                    ? raw.get("receiverTableNum")
                    : raw.get("receiverTable");
            int receiverTable = Integer.parseInt(receiverRaw.toString());

            // ★ senderTableNum 파싱 (필드명 방어)
            Object senderRaw = raw.get("senderTableNum") != null
                    ? raw.get("senderTableNum")
                    : raw.get("senderTable");
            int senderTableNum = senderRaw != null ? Integer.parseInt(senderRaw.toString()) : 0;

            String msg = (String) raw.get("message");

            // 알람 ON/OFF 체크
            String alarmKey = "alarm:" + storeId + ":" + receiverTable;
            String status = redisTemplate.opsForValue().get(alarmKey);
            if ("OFF".equals(status)) return;

            // ★ senderTableNum 포함해서 SSE 전송
            SseChatAlarmDto dto = SseChatAlarmDto.builder()
                    .storeId(storeId)
                    .senderTableNum(senderTableNum)   // ★ 추가
                    .receiverTable(receiverTable)
                    .message(msg)
                    .build();

            String data = objectMapper.writeValueAsString(dto);
            String receiverKey = storeId + "-" + receiverTable;
            SseEmitter sseEmitter = sseChatAlarmRegistry.getEmitter(receiverKey);

            if (sseEmitter != null) {
                sseEmitter.send(SseEmitter.event()
                        .name("chat-alarm")
                        .data(data));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 알람설정 기본 ON
    public void setToggleAlarmStatus(Long storeId, int tableNum, boolean on) {
        String alarmKey = "alarm:" + storeId + ":" + tableNum;
        redisTemplate.opsForValue().set(alarmKey, on ? "ON" : "OFF");
    }
}
