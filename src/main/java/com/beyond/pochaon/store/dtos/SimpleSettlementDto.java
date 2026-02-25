package com.beyond.pochaon.store.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimpleSettlementDto {
    private int dayTotal;
    private int orderCount;
    private int averageOrderAmount;

    // ── 신규 필드 ──
    private int cancelCount;
    private int refundAmount;
    private int netSales;
    private int cardSales;
    private int cashSales;
    private int transferSales;
    private int tableUseCount;
}
