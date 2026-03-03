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
    private List<CreateDetailDto> createDetailList = new ArrayList<>();

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class CreateDetailDto {
        private Long menuId;
        private int menuQuantity;
        @Builder.Default
        private List<CreateOptionDto> optionList = new ArrayList<>();


        @AllArgsConstructor
        @NoArgsConstructor
        @Data
        @Builder
        public static class CreateOptionDto {
            private Long optionId;
            @Builder.Default
            private List<CreateOptionDetailDto> optionDetailList = new ArrayList<>();


            @AllArgsConstructor
            @NoArgsConstructor
            @Data
            @Builder
            public static class CreateOptionDetailDto {
                private Long optionDetailId;
                private int optionDetailQuantity;
            }
        }
    }

}