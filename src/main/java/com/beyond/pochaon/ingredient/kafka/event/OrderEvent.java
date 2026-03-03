package com.beyond.pochaon.ingredient.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    private Long menuId;
    private int quantity;
    private Long storeId;
    private Long orderingId;
}
