package com.beyond.pochaon.cart.dto.cart_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartPaymentRequestDto {

    //테이블 정보
    private Long tableId;
    private UUID groupId;

    //    장바구니 아이템 목록
    private List<CartItem> cartItems;


    //    장바구니 아이템
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CartItem {
        private Long menuId; //메뉴 Id
        private String menuName; //메뉴 이름
        private int quantity; //수량
        private int price; // 가격 (메뉴 가격 + 옵션 가격)
        private List<Long> optionIds; // 선택한 옵션 ID리스트
    }

}
