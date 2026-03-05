package com.beyond.pochaon.ingredient.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustReqDto {
    private int actualQuantity; // 실제 창고에 있는 수량
    private String reason;      // 조정 사유 (예: "파손", "분실", "실사 후 조정")
}
