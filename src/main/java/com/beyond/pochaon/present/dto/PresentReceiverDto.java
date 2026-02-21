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
public class PresentReceiverDto {
    private int senderTableNum;
    private UUID groupId;
    private int receiverTableNum;
    private List<MenuDto> menuList;

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class MenuDto {
        private String menuName;
        private int menuQuantity;
        private String imageUrl;

    }
}