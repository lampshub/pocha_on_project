package com.beyond.pochaon.payment.dto;

import com.beyond.pochaon.payment.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


public class PaymentDto {


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
//    주문 생성 요청  프론트 -> 백엔드로
    public static class OrderCreateRequest {
        private int amount;
        private String orderName;
        private Long tableId;
        private Integer tableNum;
        private String groupId;
        private String payerType;
        private Long storeId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
//    주문 생성 응답 (백엔드 -> 프론트로)
    public static class OrderCreateResponse {
        private String orderId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
//    결제 승인 요청 (프론트 -> 백엔드 -> 토스)
    public static class ConfirmRequest {
        private String paymentKey;
        private String orderId;
        private int amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
//    결제 승인 응답  (토스 -> 백엔드 -> 프론트)
    public static class ConfirmResponse {
        private String paymentKey;
        private String orderId;
        private String orderName;
        private int totalAmount;
        private String method;
        private PaymentStatus status;
        private String approvedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorResponse {
        private String code;
        private String message;
    }
}
