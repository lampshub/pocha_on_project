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
    private List<CartDetailDto> cartDetailDto= new ArrayList<>();
    private int CartTotalPrice; //+=linePrice

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class CartDetailDto {
        private Long menuId;
        private String menuName;
        private String fieldKey;
        private int lineTotalPrice; //unitPrice +수량
        private int menuQuantity;
        @Builder.Default
        private List<CartOptionDto> cartOptionDtoList= new ArrayList<>();
    }
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class CartOptionDto {
        private String optionName;
        @Builder.Default
        private List<String> optionDetailNameList= new ArrayList<>();

    }

}
