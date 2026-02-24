package com.beyond.pochaon.present.dto;


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
        private int menuQuantity;
    }
}
