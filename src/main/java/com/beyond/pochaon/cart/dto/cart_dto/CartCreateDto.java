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
public class CartCreateDto {
    private Long tableId;
    @Builder.Default
    private List<CartCreateDetailDto> createDetailDto= new ArrayList<>();

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class CartCreateDetailDto {
        private Long menuId;
        private int menuQuantity;
        @Builder.Default
        private List<CreateOptionId> optionId= new ArrayList<>();
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class CreateOptionId {
        private Long optionId;
        @Builder.Default
        private List<Long> optionDetailId= new ArrayList<>();
    }
}

