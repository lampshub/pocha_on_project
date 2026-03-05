package com.beyond.pochaon.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosConfirmReqDto {
    private int tableNum;
    private Long storeId;
    private int amount;
    private String orderName;
}
