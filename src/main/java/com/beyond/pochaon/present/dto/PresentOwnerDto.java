package com.beyond.pochaon.present.dto;


import com.beyond.pochaon.menu.domain.OrderAlarmTo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PresentOwnerDto {
    private Long orderingId;
    private int senderTableNum;
    @Builder.Default
    private String type = "PRESENT";
    private UUID groupId;
    private int receiverTableNum;
    private List<MenuDto> menuDtoList;

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class MenuDto {
        private String menuName;
        private OrderAlarmTo orderAlarmTo;
        private int menuPrice;
        private int menuQuantity;
    }
}
