package com.beyond.pochaon.common.kafka;

import com.beyond.pochaon.menu.domain.OrderAlarmTo;
import com.beyond.pochaon.present.dto.EventQueDto;
import com.beyond.pochaon.present.dto.OwnerEventDto;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class KafkaService {
    private final KafkaTemplate<String, Object>kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaService(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }
//  일반주문, 선물주문
    public void orderCreate(OwnerEventDto eventDto, Long storeId){
        try{
            String data = objectMapper.writeValueAsString(eventDto); //일반주문, 선물주문
            kafkaTemplate.send("order-topic",storeId.toString(), data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void orderQueueCreate(EventQueDto eventQueDto, Long storeId){
        try{
            String data = objectMapper.writeValueAsString(eventQueDto); //일반주문, 선물주문
            kafkaTemplate.send("orderQue-topic",storeId.toString(), data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
