package com.beyond.pochaon.owner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementKeyReqDto {
    private String settlementKey; // 프론트에서 입력한 평문 정산키
}
