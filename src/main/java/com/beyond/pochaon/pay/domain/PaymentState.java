package com.beyond.pochaon.pay.domain;

public enum PaymentState {
    PENDING,    // 결제 대기
    COMPLETED,  // 결제 완료
    CANCELED    // 결제 취소
}