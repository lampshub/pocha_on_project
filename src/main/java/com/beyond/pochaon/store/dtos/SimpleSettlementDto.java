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
}
