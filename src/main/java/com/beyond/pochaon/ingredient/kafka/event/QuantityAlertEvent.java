package com.beyond.pochaon.ingredient.kafka.event;

import com.beyond.pochaon.ingredient.domain.StockStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuantityAlertEvent {
    private Long ingredientId;
    private String ingredientName;
    private int currentStock;
    private int safetyStock;
    private String message;
    private StockStatus status;
}
