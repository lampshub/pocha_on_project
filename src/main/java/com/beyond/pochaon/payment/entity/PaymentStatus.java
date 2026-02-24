package com.beyond.pochaon.payment.entity;

public enum PaymentStatus {
    READY, // 결제 준비(주문은 생성됐지만, 아직 승인 전)
    DONE,
    FAILED,
    CANCELED
}
