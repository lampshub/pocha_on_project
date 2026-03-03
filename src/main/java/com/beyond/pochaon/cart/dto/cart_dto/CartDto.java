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
public class CartDto {
    @Builder.Default
    private List<DetailDto> cartDetailList = new ArrayList<>();
    private int cartTotalPrice; //+=linePrice

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class DetailDto {
        private Long menuId;
        private String menuName;
        private String fieldKey;
        private int lineTotalPrice; //unitPrice +수량
        private int menuPrice;
        private int menuQuantity;
        @Builder.Default
        private List<OptionDto> optionList = new ArrayList<>();

        @AllArgsConstructor
        @NoArgsConstructor
        @Data
        @Builder
        public static class OptionDto {
            private String optionName;
            @Builder.Default
            private List<OptionDetailDto> optionDetailList = new ArrayList<>();


            @AllArgsConstructor
            @NoArgsConstructor
            @Data
            @Builder
            public static class OptionDetailDto {
                private String optionDetailName;
                private int optionDetailPrice;
                private int optionDetailQuantity;
            }
        }
    }
}