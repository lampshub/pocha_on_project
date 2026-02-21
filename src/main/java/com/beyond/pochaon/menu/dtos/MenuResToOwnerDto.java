package com.beyond.pochaon.menu.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// owner 설정관리 - 메뉴수정시 기존정보 세팅값
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class MenuResToOwnerDto {

    private String menuName;
    private int price;
    private String origin;
    private String explanation;
    private String imageUrl;
    private Long categoryId;
    private String categoryName;
    private List<OptionDto> options;

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class OptionDto {
        private Long optionId;
        private String optionName;
        private List<OptionDetailDto> details;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class OptionDetailDto {
        private Long optionDetailId;
        private String optionDetailName;
        private int optionDetailPrice;
    }
}
