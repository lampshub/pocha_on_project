package com.beyond.pochaon.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySettlementResDto {

    private int year;
    private int month;

    // ── 월간 합계 ──

    private int totalAmount;
    private int orderCount;
    private int averageOrderAmount;
    private int dailyAverageSales;
    private int cancelCount;
    private int refundAmount;
    private int netSales;
    private int tableUseCount;

    // ── 원가 / 이익 / 세금 ──

    private int totalCost;
    private int grossProfit;
    private double grossProfitRate;
    private int vat;
    private int netProfit;
    private double netProfitRate;

    // ── 결제수단별 ──

    private int cardAmount;
    private int cardCount;
    private int transferAmount;
    private int transferCount;
    private int easyPayAmount;
    private int easyPayCount;
    private int phoneAmount;
    private int phoneCount;

    // ── 일별 매출 (캘린더용) { 1: 150000, 2: 230000, ... } ──

    private Map<Integer, Integer> dailySales;

    // ── 일별 순이익 (캘린더용) { 1: 120000, 2: 180000, ... } ──

    private Map<Integer, Integer> dailyNetProfit;

    // ── 주간 breakdown ──

    private List<WeeklyItem> weeklyBreakdown;

    // ── ★ 최근 12개월 비교 트렌드 ──

    private List<MonthTrendItem> monthlyTrend;

    // ── 메뉴 순위 ──

    private List<MenuRankItem> menuRankByCount;
    private List<MenuRankItem> menuRankByAmount;

    // ── 카테고리 순위 ──

    private List<CategoryRankItem> categoryRank;

    // ── 테이블별 ──

    private List<TableStatItem> tableStats;

    // ═══════════ 내부 DTO ═══════════

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class WeeklyItem {
        private int week;
        private LocalDate weekStart;
        private LocalDate weekEnd;
        private int totalAmount;
        private int orderCount;
    }

    /** 최근 12개월 비교용 (이번달 포함, 과거 11개월) */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MonthTrendItem {
        private int year;
        private int month;
        private String label;           // "2025.06"
        private int totalAmount;
        private int orderCount;
        private int averageOrderAmount;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MenuRankItem {
        private int rank;
        private String menuName;
        private String categoryName;
        private int salesCount;
        private int salesAmount;
        private int cost;
        private int profit;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CategoryRankItem {
        private int rank;
        private String categoryName;
        private int salesCount;
        private int salesAmount;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TableStatItem {
        private int tableNum;
        private int useCount;
        private int salesAmount;
        private int orderCount;
    }
}