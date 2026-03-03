package com.beyond.pochaon.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllStoreDayResDto {

    // ① 전체 합산 요약
    private int totalRevenue;
    private int totalNetProfit;
    private int totalOrders;
    private int totalCancels;
    private int avgOrderAmount;
    private int storeCount;

    // ② 매출 비교 (전일/전주 동일 요일/전월 동일 일자)
    private int todayRevenue;
    private int todayNetProfit;
    private int yesterdayRevenue;
    private int lastWeekRevenue;
    private int lastMonthRevenue;

    // ③ 매장별 매출 랭킹
    private List<StoreDayItem> storeRanking;

    // ④ 매장별 성장률 (전일 대비)
    private List<StoreGrowthItem> storeGrowth;

    // ⑤ 매장별 객단가
    private List<StoreDayItem> storeAvgAmount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreDayItem {
        private Long storeId;
        private String storeName;
        private int revenue;
        private int orderCount;
        private int avgAmount;       // 객단가
        private int avgDailyRevenue; // 해당 월 일평균 매출
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreGrowthItem {
        private Long storeId;
        private String storeName;
        private int today;
        private int yesterday;
        private double growthRate;
    }
}