package com.beyond.pochaon.common.service;

import com.beyond.pochaon.common.dtos.SseMessageDto;
import com.beyond.pochaon.common.repository.SseEmitterRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Component
@Slf4j
public class SseAlramService implements MessageListener {
    private final SseEmitterRegistry sseEmitterRegistry;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public SseAlramService(SseEmitterRegistry sseEmitterRegistry, ObjectMapper objectMapper, @Qualifier("ssePubSub") RedisTemplate<String, String> redisTemplate) {
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    //    점주 -> 특정 테이블에 메시지 전송. emitter가 있으면 직접 전송 없으면 pub/sub
    public void sendMessage(String storeId, String tableNum, String message) {
        SseEmitter sseEmitter = sseEmitterRegistry.getEmitter(storeId, tableNum);
        SseMessageDto dto = SseMessageDto.builder()
                .storeId(storeId)
                .tableNum(tableNum)
                .message(message)
                .build();

        try {
            String data = objectMapper.writeValueAsString(dto);
            if (sseEmitter != null) {
                try {

                    sseEmitter.send(SseEmitter.event().name("staffcall").data(data));
                } catch (IOException e) {
                    log.warn("전송실패");
                    sseEmitterRegistry.removeEmitter(storeId, tableNum);
                    redisTemplate.convertAndSend("staffcall-channel", data);
                }
            } else {
                redisTemplate.convertAndSend("staffcall-channel", data);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    //    redis pub/sub 구독 콜백. 다른 서버에서 convertAndSend로 보낸 메시지를 수신해서 점주 emitter로 전달
    @Override
    public void onMessage(Message message, @Nullable byte[] pattern) {
        String channelName = new String(pattern);
        try {
            SseMessageDto dto = objectMapper.readValue(message.getBody(), SseMessageDto.class);
            SseEmitter sseEmitter = sseEmitterRegistry.getOwnerEmitter(dto.getStoreId());
            if (sseEmitter != null) {
                try {
                    String data = objectMapper.writeValueAsString(dto);
                    sseEmitter.send(SseEmitter.event().name("staffcall").data(data));
                } catch (IOException e) {
                    log.warn("전송 실패 storeId = {}", dto.getStoreId());
                    sseEmitterRegistry.removeOwnerEmitter(dto.getStoreId(), sseEmitter);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }


    }

    //    점주에게 알림 전송. 없으면 redis staffcall-channel로 pub
    public void sendToOwner(String storeId, String tableNum, String message) {
        SseEmitter ownerEmitter = sseEmitterRegistry.getOwnerEmitter(storeId);
        SseMessageDto dto = SseMessageDto.builder()
                .storeId(storeId)
                .tableNum(tableNum)
                .message(message)
                .build();
        try {
            String data = objectMapper.writeValueAsString(dto);
            if (ownerEmitter != null) {
                try {
                    ownerEmitter.send(SseEmitter.event().name("staffcall").data(data));
                } catch (IOException e) {
                    log.warn("owner emitter전송 실패, storeId = {}", storeId);
                    sseEmitterRegistry.removeOwnerEmitter(storeId, ownerEmitter);

                    redisTemplate.convertAndSend("staffcall-channel", data);
                }
            } else {
                redisTemplate.convertAndSend("staffcall-channel", data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendToOwner2(String storeId, String eventName, Object data) {
        SseEmitter emitter = sseEmitterRegistry.getOwnerEmitter(storeId); // 기존 구조에 맞게 수정
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException e) {
            sseEmitterRegistry.removeOwnerEmitter(storeId, emitter);
        }
    }


    // SseAlramService.java
    public void sendTableStatus(Long storeId, int tableNum, String status) {
        Map<String, Object> payload = Map.of(
                "tableNum", tableNum,
                "status", status
        );
        try {
            String json = objectMapper.writeValueAsString(payload);
            sseEmitterRegistry.broadcastToOwner(String.valueOf(storeId), "TABLE_STATUS", json);
        } catch (JsonProcessingException e) {
            log.error("TABLE_STATUS 직렬화 실패", e);
        }
    }
}
