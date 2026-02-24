package com.beyond.pochaon.customerorder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreateDto {
    private Long orderingId;
    private int tableNum;
    private UUID idempotencyKey;
    private UUID groupId;
    @Builder.Default
    private List<WebMenu> webMenuList = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WebMenu {
        private String menuName;
        private int quantity;
        private int menuPrice;
        @Builder.Default
        private List<Option> optionList = new ArrayList<>();

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class Option {
            private String optionGroupName;
            @Builder.Default
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
