package com.beyond.pochaon.pay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 카카오페이 금액 정보 공통 DTO
 * 모든 금액 관련 정보에서 재사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmountInfoDto {

    @JsonProperty("total")
    private Integer total;

    @JsonProperty("tax_free")
    private Integer taxFree;

    @JsonProperty("vat")
    private Integer vat;

    @JsonProperty("point")
    private Integer point;

    @JsonProperty("discount")
    private Integer discount;

    @JsonProperty("green_deposit")
    private Integer greenDeposit;
}