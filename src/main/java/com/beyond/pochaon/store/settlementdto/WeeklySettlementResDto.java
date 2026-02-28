package com.beyond.pochaon.store.settlementdto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklySettlementResDto {

    private LocalDate weekStart;
    private LocalDate weekEnd;

    // ── 주간 합계 ──

    private int totalAmount;
    private int orderCount;
    private int averageOrderAmount;
    private int cancelCount;
    private int refundAmount;
    private int netSales;
    private int tableUseCount;

    // ── 결제수단별 (StoreSettlement 합산) ──

    private int cardAmount;
    private int cardCount;
    private int transferAmount;
    private int transferCount;
    private int easyPayAmount;
    private int easyPayCount;
    private int phoneAmount;
    private int phoneCount;

    // ── 일별 (월~일) ──

    private List<DailyItem> dailyBreakdown;

    // ── 메뉴 순위 (합산) ──

    private List<MenuRankItem> menuRankByCount;
    private List<MenuRankItem> menuRankByAmount;

    // ── 카테고리 순위 ──

    private List<CategoryRankItem> categoryRank;

    // ── 테이블별 ──

    private List<TableStatItem> tableStats;

    // ═══════════ 내부 DTO ═══════════

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DailyItem {
        private LocalDate date;
        private String dayOfWeek;
        private int totalAmount;
        private int orderCount;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MenuRankItem {
        private int rank;
        private String menuName;
        private String categoryName;
        private int salesCount;
        private int salesAmount;
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
        private int avgUsageMinutes;
    }
}