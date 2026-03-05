package com.beyond.pochaon.common.service;


import com.beyond.pochaon.common.dto.SseMessageDto;
import com.beyond.pochaon.common.repository.SseEmitterRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Service
@Transactional
@Slf4j
public class KafkaStaffCallService {
    private final SseEmitterRegistry sseEmitterRegistry;
    private final ObjectMapper objectMapper;

    @Autowired
    public KafkaStaffCallService(SseEmitterRegistry sseEmitterRegistry, ObjectMapper objectMapper) {
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.objectMapper = objectMapper;
    }

    public void notifyOwner(SseMessageDto dto) {
        SseEmitter ownerEmitter = sseEmitterRegistry.getOwnerEmitter(dto.getStoreId());
        if (ownerEmitter == null) {
            log.warn("점주 sse연결 안됨_common_ser_kafkaStaffCall , storeId = {}", dto.getStoreId());
            return;
        }
        try {
            String data = objectMapper.writeValueAsString(dto);
            ownerEmitter.send(SseEmitter.event().name("staffcall").data(data));
            log.info("직원호출 알림 전송 : storeId = {}, tableNum= {}", dto.getStoreId(), dto.getTableNum());
        } catch (IOException e) {
            log.warn("owner emitter 전송 실패, storeId = {}", dto.getStoreId());
            sseEmitterRegistry.removeOwnerEmitter(dto.getStoreId(), ownerEmitter);
        }
    }
}
