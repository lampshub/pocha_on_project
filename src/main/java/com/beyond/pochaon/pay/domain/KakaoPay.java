package com.beyond.pochaon.pay.domain;

import com.beyond.pochaon.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "kakao_pay")
public class KakaoPay extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 카카오페이가 ready API 응답으로 발급해주는 결제 고유번호
    // 승인/취소 시 이 값으로 어떤 결제 건인지 식별
    @Column(unique = true, nullable = false)
    private String tid;

    // 우리 서버에서 만든 주문 식별자 (예: "ORDER_1_1738900000000")
    // approval_url에 포함시켜서 콜백 시 어떤 결제 건인지 찾는 용도
    @Column(nullable = false)
    private String partnerOrderId;

    // 결제 요청자 식별자 (예: "TABLE_1")
    // 카카오페이 API에서 요구하는 필수값
    @Column(nullable = false)
    private String partnerUserId;

    // 결제 금액 (장바구니 총액)
    @Column(nullable = false)
    private Integer amount;

    // 카카오페이 결제 화면에 보이는 상품명 (예: "짜장면 외 2건")
    private String itemName;

    // 상품 수량 (장바구니 항목 수)
    private Integer quantity;

    // 결제 요청한 테이블 ID
    // 승인 콜백 시 이 테이블의 장바구니를 읽어서 주문을 생성함
    @Column(nullable = false)
    private Long tableId;

    // 결제 진행 상태 (READY → APPROVED → CANCELED/FAILED)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private KakaoPayStatus status = KakaoPayStatus.READY;

    // 카카오페이 승인 완료 시각
    private LocalDateTime approvedAt;

    // 카카오페이 취소 완료 시각
    private LocalDateTime canceledAt;

    // ready 응답으로 받는 결제 페이지 URL들
    // 손님이 이 URL로 접속해서 결제 진행 (QR코드 = mobileUrl)
    @Column(length = 500)
    private String nextRedirectPcUrl;      // PC 웹 결제 URL
    @Column(length = 500)
    private String nextRedirectMobileUrl;  // 모바일 웹 결제 URL → QR코드 데이터로 사용
    @Column(length = 500)
    private String nextRedirectAppUrl;     // 앱 결제 URL

    // 카카오페이 승인 시 받는 요청 고유번호
    // approve 응답에 포함되어 오는 값
    private String aid;

    // 취소된 총 금액 (부분 취소 대비, 현재는 전액 취소만 사용)
    @Builder.Default
    private Integer cancelAmount = 0;

    // 취소 사유 (예: "사용자 요청", "품절")
    @Column(length = 500)
    private String cancelReason;

//    결제 대상 groupID
    @Column(columnDefinition = "BINARY(16)")
    private UUID groupId;

//    결제 대상 주문 ID들 (쉼표로 구분함: "1,2,3"
    @Column(length = 1000)
    private String orderingIds;


    // === 비즈니스 메서드 ===

    // 결제 승인 처리: 상태를 APPROVED로 변경하고 승인 정보 저장
    public void approve(String aid, LocalDateTime approvedAt) {
        this.aid = aid;
        this.status = KakaoPayStatus.APPROVED;
        this.approvedAt = approvedAt;
    }

    // 결제 취소 처리: 상태를 CANCELED로 변경하고 취소 정보 저장
    public void cancel(Integer cancelAmount, String cancelReason, LocalDateTime canceledAt) {
        this.cancelAmount += cancelAmount;
        this.cancelReason = cancelReason;
        this.status = KakaoPayStatus.CANCELED;
        this.canceledAt = canceledAt;
    }

    // 결제 실패 처리
    public void fail() {
        this.status = KakaoPayStatus.FAILED;
    }


}