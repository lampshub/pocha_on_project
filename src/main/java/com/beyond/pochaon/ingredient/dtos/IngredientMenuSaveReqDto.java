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
public class IngredientMenuSaveReqDto {
    private Long menuId;
    private List<IngredientUsageDto> ingredientUsageList;
}
