package com.beyond.pochaon.store.settlementdto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailySettlementResDto {

    private LocalDate date;

    // ── 기본 정산 ──

    private int totalAmount;
    private int orderCount;
    private int averageOrderAmount;
    private int cancelCount;
    private int refundAmount;
    private int netSales;
    private int tableUseCount;

    // ── 결제수단별 ──

    private int cardAmount;
    private int cardCount;
    private int transferAmount;
    private int transferCount;
    private int easyPayAmount;
    private int easyPayCount;
    private int phoneAmount;
    private int phoneCount;

    // ── 메뉴 순위 ──

    private List<MenuRankItem> menuRankByCount;
    private List<MenuRankItem> menuRankByAmount;

    // ── 카테고리 순위 ──

    private List<CategoryRankItem> categoryRank;

    // ── 테이블별 ──

    private List<TableStatItem> tableStats;

    // ── 주문 내역 ──

    private List<OrderItem> orders;

    // ═══════════ 내부 DTO ═══════════

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
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrderItem {
        private Long orderingId;
        private int tableNum;
        private int totalPrice;
        private String orderStatus;
        private String paymentMethod;
        private LocalDateTime orderedAt;
        private List<OrderMenuItem> menus;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrderMenuItem {
        private String menuName;
        private int quantity;
        private int price;
        private List<String> options;
    }
}