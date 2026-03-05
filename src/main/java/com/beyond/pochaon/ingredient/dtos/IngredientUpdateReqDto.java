package com.beyond.pochaon.ingredient.dtos;

import com.beyond.pochaon.ingredient.domain.IngredientType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientUpdateReqDto {
    private String name;
    private IngredientType type;
    private Integer safetyStock;
    private String unit;
}
