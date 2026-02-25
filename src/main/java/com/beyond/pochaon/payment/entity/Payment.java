package com.beyond.pochaon.payment.entity;

import com.beyond.pochaon.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Payment extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //    토스페이먼츠 주문 Id
    @Column(nullable = false, unique = true)
    private String orderId;

    //    토스페이먼츠 paymentKey (승인 후 저장)
    @Column(unique = true)
    private String paymentKey;

    //    주문명
    @Column(nullable = false)
    private String orderName;

    //    결제 금액
    @Column(nullable = false)
    private int amount;

    // 결제 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.READY;

    //    결제 수단  (카드 계좌이체 등 )
    private String method;

    //    결제자 유형
    @Enumerated(EnumType.STRING)
    private PayerType payerType;

    //    테이블 번호
    private Integer tableNum;

    //    주문 그룹 Id
    private String groupId;

    //  결제 승인 시각(토스에서 주는 값)
    private LocalDateTime approveAt;

    @Column(nullable = false)
    private Long storeId;

    public void approve(String paymentKey, String method, LocalDateTime approveAt) {
        this.paymentKey = paymentKey;
        this.method = method;
        this.approveAt = approveAt;
        this.status = PaymentStatus.DONE;
    }

    public void fail() {
        this.status = PaymentStatus.FAILED;
    }

    public void cancel() {
        this.status = PaymentStatus.CANCELED;
    }
}
