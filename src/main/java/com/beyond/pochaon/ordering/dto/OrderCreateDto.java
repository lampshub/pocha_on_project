package com.beyond.pochaon.ordering.dto;

import com.beyond.pochaon.menu.domain.OrderAlarmTo;
import lombok.*;

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
        private Long menuId;
        private OrderAlarmTo orderAlarmTo;
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
            private Long optionId;
            private String optionName;
            @Builder.Default
            private List<OptionDetail> optionDetailList = new ArrayList<>();


            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            @Builder
            public static class OptionDetail {
                private Long optionDetailId;
                private String optionDetailName;
                private int optionDetailPrice;
                private int optionDetailQuantity;
            }

        }

    }
}
