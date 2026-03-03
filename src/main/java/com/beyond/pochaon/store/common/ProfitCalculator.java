package com.beyond.pochaon.store.common;

/**
 * 순이익 계산 유틸 — 매출 기반 공통 공식
 *
 * 순이익 = 매출 - 재료비 - 부가세(매출/11)
 */
public class ProfitCalculator {

    private ProfitCalculator() {}

    /** 순이익 */
    public static int netProfit(int revenue, int cost) {
        return revenue - cost - vat(revenue);
    }

    /** 부가세 */
    public static int vat(int revenue) {
        return Math.round(revenue / 11f);
    }

    /** 매출이익 (세전) */
    public static int grossProfit(int revenue, int cost) {
        return revenue - cost;
    }

    /** 비율 (%) — 소수 첫째 자리 */
    public static double rate(int part, int total) {
        if (total == 0) return 0;
        return Math.round(part * 1000.0 / total) / 10.0;
    }
}