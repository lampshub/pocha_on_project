package com.beyond.pochaon.store.settlementdto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesAnalysisResDto {

    private List<SalesBar> dailyBars;
    private List<SalesBar> weeklyBars;
    private List<SalesBar> monthlyBars;
    private List<HourlySales> hourlySales;
    private List<SalesBar> dayOfWeekSales;
    private SalesCompare compare;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesBar {
        private String label;
        private int value;
        private int height;   // 최대값 대비 %
        private boolean best;  // 최고 매출 여부
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlySales {
        private int hour;
        private int height;  // 최대값 대비 %
        private int amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesCompare {
        private int thisMonth;
        private int lastMonth;
        private double momRate;   // 전월 대비 증감률 (%)
        private int lastYear;
        private double yoyRate;   // 전년 동월 대비 증감률 (%)
    }
}