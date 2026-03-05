package com.beyond.pochaon.common.kafka;

import com.beyond.pochaon.common.dto.SseMessageDto;
import com.beyond.pochaon.common.service.KafkaStaffCallService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StaffCallConsumer {
    private final KafkaStaffCallService kafkaStaffCallService;
    private final ObjectMapper objectMapper;

    @Autowired
    public StaffCallConsumer(KafkaStaffCallService kafkaStaffCallService, ObjectMapper objectMapper) {
        this.kafkaStaffCallService = kafkaStaffCallService;
        this.objectMapper = objectMapper;
    }

//consumer
    @KafkaListener(topics = "staff-call-topic", groupId = "staff-call-group")
    public void consume(String message) {
        try {
            SseMessageDto dto = objectMapper.readValue(message, SseMessageDto.class);
            kafkaStaffCallService.notifyOwner(dto);
        } catch (Exception e) {
            log.error("직원호출 메시지 처리 실패/  {}", e.getMessage());
        }

    }
}
