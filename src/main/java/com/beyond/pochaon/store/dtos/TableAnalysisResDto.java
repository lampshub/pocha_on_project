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
public class TableAnalysisResDto {

    private TableSummary summary;
    private List<TableSales> tableSales;
    private List<TableTurnover> tableTurnover;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableSummary {
        private double avgTurnover;    // 평균 회전율 (회/일)
        private int avgDuration;       // 평균 이용 시간 (분)
        private int todayUseCount;     // 오늘 총 이용 횟수
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableSales {
        private int tableNum;
        private int amount;
        private int count;
        private int rate;  // 최대 대비 비율 (%)
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableTurnover {
        private int tableNum;
        private double turnover;
    }
}