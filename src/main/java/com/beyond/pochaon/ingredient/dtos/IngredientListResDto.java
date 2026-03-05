package com.beyond.pochaon.ingredient.dtos;

import com.beyond.pochaon.ingredient.domain.IngredientType;
import com.beyond.pochaon.ingredient.domain.StockStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientListResDto {
    private Long ingredientId;
    private String name;
    private IngredientType type;
    private int currentStock;
    private int safetyStock;
    private StockStatus status;
    private String unit;
}