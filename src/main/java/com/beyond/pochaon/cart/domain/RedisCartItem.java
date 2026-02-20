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
    private String optionKey; // 내부비교
    private List<CartOption> cartOptionDtoList= new ArrayList<>();; //옵션 정보
    private int quantity;
    private int unitPrice;

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartOption {

        private String optionName;
        private List<String> optionDetailNameList= new ArrayList<>();;
    }
}