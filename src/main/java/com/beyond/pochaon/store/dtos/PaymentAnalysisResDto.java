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
public class PaymentAnalysisResDto {

    private PaymentMethodBreakdown methodBreakdown;
    private PaymentSummary summary;
    private List<TransactionDetail> recentTransactions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethodBreakdown {
        private int cardRate;       // 카드 비율 (%)
        private int easyPayRate;       // 간편결제 비율 (%)
        private int transferRate;   // 이체 비율 (%)
        private int phoneRate;   // 핸드폰 비율 (%)
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentSummary {
        private int avgAmount;
        private int totalCount;
        private int totalAmount;
        private int monthlyTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionDetail {
        private Long id;
        private String method;     // "카드", "현금", "계좌이체"
        private String time;       // "HH:mm"
        private String paymentKey; //결제 번호
        private int amount;
        private int tableNum;
        private List<TransactionMenu> menus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionMenu {
        private String name;
        private int qty;
        private int price;
    }
}