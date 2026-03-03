package com.beyond.pochaon.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllStoreSummaryResDto {

    private int totalRevenue;       // 전체 매장 총 매출
    private int totalNetProfit;     // ★ 전체 매장 총 순이익
    private int totalOrders;        // 전체 매장 총 주문 수
    private int totalCancels;       // 전체 매장 총 취소 수
    private int storeCount;         // 운영 매장 수
    private double monthGrowthRate; // 전월 대비 성장률 (순이익 기준)
    private int avgOrderAmount;     // 평균 객단가

    // ★ 달력용 일별 데이터
    private Map<Integer, Integer> dailySales;     // 일 → 매출
    private Map<Integer, Integer> dailyNetProfit;  // 일 → 순이익
}