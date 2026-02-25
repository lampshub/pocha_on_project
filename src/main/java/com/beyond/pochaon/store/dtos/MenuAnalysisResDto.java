package com.beyond.pochaon.store.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuAnalysisResDto {

    private List<CategorySales> categorySales;
    private List<MenuRank> menuRanking;
    private List<MenuCombo> combos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySales {
        private String name;
        private int amount;
        private int rate;  // 최대 대비 비율 (%)
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuRank {
        private String name;
        private int qty;
        private int amount;
        private int rate;  // 전체 대비 비율 (%)
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuCombo {
        private String pair;
        private int count;
    }
}