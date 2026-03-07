package com.beyond.pochaon.ordering.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class MenuDoneDto {
    private UUID menuDoneId;
    private Long orderingId;
    private int tableNum;
    private String menuName;
    private int menuQuantity;
    private int menuTotal;  //총 메뉴수(점주가 서빙완료 done처리 시점 잡기위함)
    private List<OptionDto> menuOptionlist;
    private LocalDateTime createAt; // 주문 시간 (basetime으로 생성된 시간)
    private Long storeId;               // Kafka key 용

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class OptionDto{
        private String optionName;
        private List<OptionDetailDto> optionDetailDtoList;

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