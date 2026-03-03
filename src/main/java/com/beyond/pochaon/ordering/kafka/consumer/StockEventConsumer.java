package com.beyond.pochaon.ordering.kafka.consumer;

import com.beyond.pochaon.ingredient.kafka.event.QuantityDecreaseFailedEvent;
import com.beyond.pochaon.menu.domain.Menu;
import com.beyond.pochaon.menu.domain.MenuStatus;
import com.beyond.pochaon.menu.repository.MenuRepository;
import com.beyond.pochaon.ordering.domain.OrderStatus;
import com.beyond.pochaon.ordering.domain.Ordering;
import com.beyond.pochaon.ordering.repository.OrderingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Component
public class StockEventConsumer {
    private final OrderingRepository orderingRepository;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final MenuRepository menuRepository;

    public StockEventConsumer(OrderingRepository orderingRepository, ObjectMapper objectMapper, SimpMessagingTemplate messagingTemplate, MenuRepository menuRepository) {
        this.orderingRepository = orderingRepository;
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
        this.menuRepository = menuRepository;
    }

    @KafkaListener(topics = "stock-decrease-failed", groupId = "order-group")
    public void handleStockFailure(String message, Acknowledgment ack) {
        try {
            // 1. 메시지 역직렬화
            QuantityDecreaseFailedEvent event = objectMapper.readValue(message, QuantityDecreaseFailedEvent.class);
            log.warn(">>> 재고 차감 실패 수신: 주문ID {}, 사유: {}", event.getOrderingId(), event.getReason());

            // 2. 주문 상태 변경 (보상 트랜잭션)
            cancelOrder(event);

            // 3. 메시지 처리 완료 알림
            ack.acknowledge();

        } catch (Exception e) {
            log.error("실패 이벤트 처리 중 오류 발생: {}", e.getMessage());
            // 에러 시 ack를 하지 않아 재처리를 유도하거나 DLQ로 보낼 수 있음
        }
    }

    @Transactional
    public void cancelOrder(QuantityDecreaseFailedEvent event) {
        // 1. 주문 취소 처리
        Ordering ordering = orderingRepository.findById(event.getOrderingId())
                .orElseThrow(() -> new EntityNotFoundException("주문 없음"));

        if (ordering.getOrderStatus() != OrderStatus.CANCELLED) {
            ordering.updateOrderStatus(OrderStatus.CANCELLED);
        }

        // 2. 메뉴 품절 처리 추가
        if (event.getMenuId() != null) {
            Menu menu = menuRepository.findById(event.getMenuId())
                    .orElseThrow(() -> new EntityNotFoundException("메뉴 없음"));

            menu.updateStatus(MenuStatus.SOLD_OUT);
            log.info(">>> 메뉴 ID {} 가 재고 부족으로 품절 처리되었습니다.", event.getMenuId());

            // 3. 점주/사용자 앱에 메뉴 상태 변경 알림
            messagingTemplate.convertAndSend("/topic/menu-status", Map.of(
                    "menuId", menu.getId(),
                    "status", "SOLD_OUT"
            ));
        }
    }

    private void sendCancellationNotice(Ordering ordering) {
        messagingTemplate.convertAndSend(
                "/topic/order-status/" + ordering.getGroupId(),
                Map.of(
                        "type", "ORDER_CANCELLED",
                        "message", "재고 부족으로 인해 주문이 자동 취소되었습니다.",
                        "orderingId", ordering.getId()
                )
        );
    }
}
