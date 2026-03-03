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
public class AllStoreCompareResDto {

    private List<StoreCompareItem> stores;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreCompareItem {
        private Long storeId;
        private String storeName;
        private int revenue;        // 해당 기간 매출
        private int orderCount;     // 주문 수
        private int avgAmount;      // 객단가
        private int thisMonth;      // 이번달 매출
        private int lastMonth;      // 전월 매출
        private double growthRate;  // 전월 대비 성장률 (%)
    }
}