package com.beyond.pochaon.ingredient.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeDetailResDto {
    private Long menuId;
    private String menuName;
    private List<IngredientUsageInfo> ingredients;

    @Data
    @Builder
    public static class IngredientUsageInfo {
        private Long ingredientId;
        private String ingredientName;
        private double usageAmount;
        private String unit;
    }
}
