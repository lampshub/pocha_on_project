package com.beyond.pochaon.menu.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class MenuDetailPageDto {
    private Long menuId;
    private String menuName;
    private int menuPrice;
    private int quantity;
    @Builder.Default
    private List<mappingOption> mappingOptionList= new ArrayList<>();

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class mappingOption {
        private Long optionId;
        private String optionName;
        @Builder.Default
        private List<mappingOptionDetail> mappingOptionDetailList= new ArrayList<>();

        @AllArgsConstructor
        @NoArgsConstructor
        @Data
        @Builder
        public static class mappingOptionDetail {
            private Long optionDetailId;
            private String optionDetailName;
            private int optionDetailPrice;
        }
    }
}
