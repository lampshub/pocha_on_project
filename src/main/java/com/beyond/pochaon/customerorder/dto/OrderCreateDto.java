package com.beyond.pochaon.customerorder.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreateDto {

    private int tableNumber;
    private UUID idempotencyKey;
    private UUID groupId;
    private List<WebMenu> webMenuList = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WebMenu {
        private String menuName;
        private int quantity;
        private List<Option> optionList = new ArrayList<>();

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class Option {
            private String optionGroupName;
            private List<OptionDetail> optionDetailList = new ArrayList<>();


            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            @Builder
            public static class OptionDetail {
                private String optionDetailName;
                private int optionDetailPrice;
            }

        }

    }
}
