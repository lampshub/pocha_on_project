package com.beyond.pochaon.ingredient.kafka.consumer;

import com.beyond.pochaon.ingredient.kafka.event.OrderEvent;
import com.beyond.pochaon.ingredient.service.IngredientService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InventoryConsumer {

    private final IngredientService ingredientService;
    private final ObjectMapper objectMapper; // Spring이 기본 제공하는 Bean 활용

    public InventoryConsumer(IngredientService ingredientService, ObjectMapper objectMapper) {
        this.ingredientService = ingredientService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "order-events", groupId = "inventory-group", containerFactory = "kafkaListener")
    public void consumeOrder(String message, Acknowledgment ack) {
        log.info(">>> 수신된 원본 메시지: {}", message);

        try {
            // 1. String -> DTO 수동 변환
            OrderEvent event = objectMapper.readValue(message, OrderEvent.class);

            // 2. 재고 차감 비즈니스 로직 수행 (FIFO + Safety Stock)
            ingredientService.decreaseIngredientQuantity(event);

            // 3. 로직 성공 시 수동 커밋
            ack.acknowledge();
            log.info(">>> 재고 차감 성공 및 오프셋 커밋 완료");

        } catch (JsonProcessingException e) {
            log.error("메시지 파싱 실패: {}", e.getMessage());
            // 잘못된 데이터 포맷이므로 커밋하고 버리거나 별도 처리
            ack.acknowledge();
        } catch (Exception e) {
            log.error("재고 처리 중 예외 발생: {}", e.getMessage());
            // 처리 실패 시 ack를 호출하지 않으면, 설정에 따라 재시도함
        }
    }
}
