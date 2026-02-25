package com.beyond.pochaon.cart.dto.cart_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CartUpdateDto {
    private Long tableId;
    private Long menuId;
    private int delta;
    @Builder.Default
    private List<Long> optionIds= new ArrayList<>();
    private String fieldKey;
}
