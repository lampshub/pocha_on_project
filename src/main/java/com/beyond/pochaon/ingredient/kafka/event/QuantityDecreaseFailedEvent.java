package com.beyond.pochaon.ingredient.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuantityDecreaseFailedEvent {
    private Long menuId;
    private Long orderingId;     // 취소해야 할 주문 ID
    private UUID idempotencyKey; // 멱등성 키)
    private String reason;       // 실패 사유
}
