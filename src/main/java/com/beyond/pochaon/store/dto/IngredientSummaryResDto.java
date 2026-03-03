package com.beyond.pochaon.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientSummaryResDto {

    private int monthlyIntakeCost;      // 이번 달 입고 총액
    private int lossCost;               // 로스 비용 (유통기한 만료 재료의 잔량 가치)
    private double lossRate;            // 로스율 (%)
    private int expiringSoonCount;      // 유통기한 임박 재료 수

    private List<LossItem> lossItems;               // 로스 재료 목록
    private List<ExpiringSoonItem> expiringSoonItems; // 임박 재료 목록

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LossItem {
        private String ingredientName;
        private int remainQuantity;     // 남은 수량
        private int unitPrice;          // 단가
        private int lossAmount;         // 로스 금액 (단가 × 남은수량)
        private LocalDateTime deadline; // 유통기한
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ExpiringSoonItem {
        private String ingredientName;
        private int remainQuantity;
        private int unitPrice;
        private int valueAtRisk;        // 위험 금액
        private LocalDateTime deadline;
        private int daysLeft;           // 남은 일수
    }
}