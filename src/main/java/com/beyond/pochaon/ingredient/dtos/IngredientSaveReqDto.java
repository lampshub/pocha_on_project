package com.beyond.pochaon.ingredient.dtos;

import com.beyond.pochaon.ingredient.domain.IngredientType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientSaveReqDto {
    private String name;
    private int safetyStock;
    private IngredientType type;   // 식재료 / 완제품
    private int quantity;          // 이번에 들어온 수량
    private int totalPrice;        // 이번 입고 총액
    private LocalDateTime deadline; // 유통기한
    private String unit;           // 측정 단위 (g, kg, ml, L, 개, 병, 팩)
}
