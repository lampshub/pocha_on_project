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
public class SettlementMenuRank extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private StoreSettlement settlement;

    // ── 메뉴 스냅샷 (메뉴 삭제돼도 정산 유지) ──

    private Long menuId;
    private String menuName;
    private String categoryName;

    // ── 판매 통계 ──

    private int salesCount;           // 판매 수량
    private int salesAmount;          // 판매 금액

    private int rankByCount;          // 건수 순위
    private int rankByAmount;         // 금액 순위
}