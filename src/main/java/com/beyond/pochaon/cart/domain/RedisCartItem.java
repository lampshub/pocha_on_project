package com.beyond.pochaon.cart.domain;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@Builder

public class RedisCartItem {
    private Long menuId;
    private String menuName;
    private int menuPrice;
    private String optionKey; // 내부비교
    private int quantity;
    @Builder.Default
    private List<CartOption> cartOptionDtoList= new ArrayList<>(); //옵션 정보



    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartOption {

        private String optionName;
        @Builder.Default
        private List<OptionDetail> optionDetailList= new ArrayList<>();


        @Getter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class OptionDetail {

            private String optionDetailName;
            private int optionDetailPrice;
            private int optionDetailQuantity;

        }
    }


}