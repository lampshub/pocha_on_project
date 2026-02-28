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
public class SettlementCategoryRank extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private StoreSettlement settlement;

    // ── 카테고리 스냅샷 ──

    private Long categoryId;
    private String categoryName;

    // ── 매출 통계 ──

    private int salesCount;           // 판매 수량
    private int salesAmount;          // 매출 합계
    private int rankByAmount;         // 매출 순위
}