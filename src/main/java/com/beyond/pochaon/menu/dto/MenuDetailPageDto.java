package com.beyond.pochaon.menu.dto;

import com.beyond.pochaon.menu.domain.SelectionType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        @Builder.Default
        @Enumerated(EnumType.STRING)
        private SelectionType selectionType = SelectionType.SINGLE;
        private String optionName;
        private Integer minSelect;
        private Integer maxSelect;
        @Builder.Default
        private List<mappingOptionDetail> mappingOptionDetailList= new ArrayList<>();

        @AllArgsConstructor
        @NoArgsConstructor
        @Data
        @Builder
        public static class mappingOptionDetail {
            private Long optionDetailId;
            private Integer maxQuantity;
            private String optionDetailName;
            private int optionDetailPrice;
        }
    }
}
