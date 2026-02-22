package com.beyond.pochaon.common.service;

import com.beyond.pochaon.common.dtos.SseMessageDto;
import com.beyond.pochaon.common.repository.SseEmitterRegistry;
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

@Component
@Slf4j
public class SseAlramService implements MessageListener {
    private final SseEmitterRegistry sseEmitterRegistry;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public SseAlramService(SseEmitterRegistry sseEmitterRegistry, ObjectMapper objectMapper, @Qualifier("ssePubSub")RedisTemplate<String, String> redisTemplate) {
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

//    특정 테이블에 메시지 전송. emitter가 있으면 직접 전송 없으면 pub/sub
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
                sseEmitter.send(SseEmitter.event().name("staffcall").data(data));
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
            String data = objectMapper.writeValueAsString(dto);
            if (sseEmitter != null) {
                sseEmitter.send(SseEmitter.event().name("staffcall").data(data));
            }
            log.info("message: {}", dto);
        } catch (IOException e) {
            e.printStackTrace();
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
                ownerEmitter.send(SseEmitter.event().name("staffcall").data(data));
            } else {
                redisTemplate.convertAndSend("staffcall-channel", data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
