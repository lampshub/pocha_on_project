package com.beyond.pochaon.pay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * QR 결제 응답 DTO
 * 클라이언트에게 QR 코드 정보를 전달하기 위한 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QRPaymentResponseDto {
    
    // 결제 고유번호
    private String tid;
    
    // PC 웹 결제 URL
    private String nextRedirectPcUrl;
    
    // 모바일 웹 결제 URL
    private String nextRedirectMobileUrl;
    
    // 앱 결제 URL
    private String nextRedirectAppUrl;
    
    // QR 코드용 데이터 (결제 URL을 QR 코드로 변환 가능)
    private String qrCodeData;
    
    // 생성 시간
    private String createdAt;
    
    // 주문 정보
    private String partnerOrderId;
    private Integer amount;
    private String itemName;
}