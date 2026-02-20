package com.beyond.pochaon.customerorder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OrderListDto {
    private Long tableId;
    private UUID groupId;
    private int totalPrice;
    private List<OrderListDetailDto> listDetailDto;

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class OrderListDetailDto {
        private Long menuId;
        private String menuName;
        private int menuQuantity;
        private int menuPrice;
        private int linePrice;
        private List<OrderListDetailOptionDto> orderDetailOpDto;

        @AllArgsConstructor
        @NoArgsConstructor
        @Data
        @Builder
        public static class OrderListDetailOptionDto {
            private Long optionId;
            private int optionPrice;
            private String optionName;
            private List<OrderDetailOptionDetailDto> orderDetailOptionDetailDto;


            @AllArgsConstructor
            @NoArgsConstructor
            @Data
            @Builder
            public static class OrderDetailOptionDetailDto {
                private Long optionDetailId;
                private int optionDetailPrice;
                private String optionDetailName;
            }

        }
    }

}


