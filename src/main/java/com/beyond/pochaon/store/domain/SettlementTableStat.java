package com.beyond.pochaon.store.domain;

import com.beyond.pochaon.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class SettlementTableStat extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private StoreSettlement settlement;

    // ── 테이블 정보 ──

    private int tableNum;

    // ── 이용 통계 ──

    private int useCount;             // 이용 횟수 (distinct groupId)
    private int salesAmount;          // 테이블 매출
    private int orderCount;           // 테이블 주문 건수
    private int avgUsageMinutes;      // 평균 이용 시간 (분)
}