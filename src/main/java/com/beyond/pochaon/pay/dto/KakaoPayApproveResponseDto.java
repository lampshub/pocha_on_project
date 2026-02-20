package com.beyond.pochaon.pay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 카카오페이 결제 승인 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KakaoPayApproveResponseDto {

    @JsonProperty("aid")
    private String aid;

    @JsonProperty("tid")
    private String tid;

    @JsonProperty("cid")
    private String cid;

    @JsonProperty("partner_order_id")
    private String partnerOrderId;

    @JsonProperty("partner_user_id")
    private String partnerUserId;

    @JsonProperty("payment_method_type")
    private String paymentMethodType;

    @JsonProperty("amount")
    private AmountInfoDto amount;

    @JsonProperty("card_info")
    private CardInfo cardInfo;

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

    @JsonProperty("payload")
    private String payload;

    /**
     * 카드 정보 (승인 응답에만 포함됨)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CardInfo {

        @JsonProperty("kakaopay_purchase_corp")
        private String kakaopayPurchaseCorp;

        @JsonProperty("kakaopay_purchase_corp_code")
        private String kakaopayPurchaseCorpCode;

        @JsonProperty("kakaopay_issuer_corp")
        private String kakaopayIssuerCorp;

        @JsonProperty("kakaopay_issuer_corp_code")
        private String kakaopayIssuerCorpCode;

        @JsonProperty("bin")
        private String bin;

        @JsonProperty("card_type")
        private String cardType;

        @JsonProperty("install_month")
        private String installMonth;

        @JsonProperty("approved_id")
        private String approvedId;

        @JsonProperty("card_mid")
        private String cardMid;

        @JsonProperty("interest_free_install")
        private String interestFreeInstall;

        @JsonProperty("installment_type")
        private String installmentType;

        @JsonProperty("card_item_code")
        private String cardItemCode;
    }
}