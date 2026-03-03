package com.beyond.pochaon.ordering.dto;

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
    private UUID groupId;
    private int totalPrice;
    private Boolean isPresent;
    private List<OrderListDetailDto> listDetailDto;

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class OrderListDetailDto {
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
            private String optionName;
            private List<OrderDetailOptionDetailDto> orderDetailOptionDetailDto;


            @AllArgsConstructor
            @NoArgsConstructor
            @Data
            @Builder
            public static class OrderDetailOptionDetailDto {
                private int optionDetailPrice;
                private String optionDetailName;
                private int optionDetailQuantity;
            }

        }
    }

}


