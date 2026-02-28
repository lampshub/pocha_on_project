package com.beyond.pochaon.common.service;

import com.beyond.pochaon.common.dtos.SseChatAlarmDto;
import com.beyond.pochaon.common.repository.SseChatAlarmRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Slf4j
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
            log.info("★ [SSE onMessage] 수신: {}", raw);

            // ★ 채팅 종료 이벤트 처리
            if ("chat-closed".equals(raw.get("type"))) {
                Long storeId = Long.valueOf(raw.get("storeId").toString());
                int table1 = Integer.parseInt(raw.get("table1Num").toString());
                int table2 = Integer.parseInt(raw.get("table2Num").toString());
                String data = objectMapper.writeValueAsString(raw);

                // 양쪽 테이블 모두에게 SSE 전송
                for (int tableNum : new int[]{table1, table2}) {
                    String key = storeId + "-" + tableNum;
                    SseEmitter emitter = sseChatAlarmRegistry.getEmitter(key);
                    log.info("★ [chat-closed] key={}, emitter존재={}", key, emitter != null);
                    if (emitter != null) {
                        emitter.send(SseEmitter.event()
                                .name("chat-closed")
                                .data(data));
                        log.info("★ [chat-closed] key={}, emitter존재={}", key, emitter != null);
                    }
                }
                return; // ★ 여기서 리턴해야 아래 일반 알림 로직을 타지 않음
            }

            // ── 기존 채팅 알림 로직 (그대로 유지) ──
            Long storeId = Long.valueOf(raw.get("storeId").toString());

            Object receiverRaw = raw.get("receiverTableNum") != null
                    ? raw.get("receiverTableNum")
                    : raw.get("receiverTable");
            int receiverTable = Integer.parseInt(receiverRaw.toString());

            Object senderRaw = raw.get("senderTableNum") != null
                    ? raw.get("senderTableNum")
                    : raw.get("senderTable");
            int senderTableNum = senderRaw != null ? Integer.parseInt(senderRaw.toString()) : 0;

            String msg = (String) raw.get("message");

            String alarmKey = "alarm:" + storeId + ":" + receiverTable;
            String status = redisTemplate.opsForValue().get(alarmKey);
            if ("OFF".equals(status)) return;

            SseChatAlarmDto dto = SseChatAlarmDto.builder()
                    .storeId(storeId)
                    .senderTableNum(senderTableNum)
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
