package com.beyond.pochaon.pay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 카카오페이 결제 취소 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KakaoPayCancelRequestDto {

    @JsonProperty("cid")
    private String cid;

    @JsonProperty("tid")
    private String tid;

    @JsonProperty("cancel_amount")
    private Integer cancelAmount;

    @JsonProperty("cancel_tax_free_amount")
    @Builder.Default
    private Integer cancelTaxFreeAmount = 0;

    @JsonProperty("cancel_vat_amount")
    private Integer cancelVatAmount;

    @JsonProperty("cancel_available_amount")
    private Integer cancelAvailableAmount;

    @JsonProperty("cancel_reason")
    private String cancelReason;
}