package com.beyond.pochaon.pay.domain;

/**
 * 카카오페이 결제 상태
 */
public enum KakaoPayStatus {
    READY,      // 결제 준비
    APPROVED,   // 결제 승인 완료
    CANCELED,   // 결제 취소
    FAILED      // 결제 실패
}