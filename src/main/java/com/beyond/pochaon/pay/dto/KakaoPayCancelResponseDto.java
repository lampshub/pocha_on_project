package com.beyond.pochaon.pay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 카카오페이 결제 취소 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KakaoPayCancelResponseDto {

    @JsonProperty("aid")
    private String aid;

    @JsonProperty("tid")
    private String tid;

    @JsonProperty("cid")
    private String cid;

    @JsonProperty("status")
    private String status;

    @JsonProperty("partner_order_id")
    private String partnerOrderId;

    @JsonProperty("partner_user_id")
    private String partnerUserId;

    @JsonProperty("payment_method_type")
    private String paymentMethodType;

    @JsonProperty("amount")
    private AmountInfoDto amount;

    @JsonProperty("approved_cancel_amount")
    private AmountInfoDto approvedCancelAmount;

    @JsonProperty("canceled_amount")
    private AmountInfoDto canceledAmount;

    @JsonProperty("cancel_available_amount")
    private AmountInfoDto cancelAvailableAmount;

    @JsonProperty("item_name")
    private String itemName;

    @JsonProperty("item_code")
    private String itemCode;

    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("approved_at")
    private String approvedAt;

    @JsonProperty("canceled_at")
    private String canceledAt;
}