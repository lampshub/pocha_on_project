package com.beyond.pochaon.store.domain;

import com.beyond.pochaon.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@ToString
@Builder
public class StoreSettlement extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long storeSettlementId; // 정산 저장 테이블

    @ManyToOne
    @JoinColumn(nullable = false)
    private Store store;

    // 정산 날짜 (영업일 기준)
    @Column(nullable = false)
    private LocalDate settlementDate;
    private int dayTotalAmount;       // 일일 총 매출
    private int orderCount;           // 주문 건수
    private int averageOrderAmount;   // 평균 객단가

    // ── 신규 필드 ──
    private int cancelCount;          // 주문 취소 건수
    private int refundAmount;         // 환불 금액
    private int netSales;             // 순매출 (총매출 - 환불)
    private int cardSales;            // 카드 결제 금액
    private int cashSales;            // 현금 결제 금액
    private int transferSales;        // 계좌이체 결제 금액
    private int tableUseCount;        // 테이블 이용 횟수


}
