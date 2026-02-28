package com.beyond.pochaon.store.domain;

import com.beyond.pochaon.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class StoreSettlement extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long storeSettlementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    // ── 기본 정산 ──

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    private int dayTotalAmount;           // 일일 총 매출
    private int orderCount;               // 주문 건수
    private int averageOrderAmount;       // 평균 객단가

    // ── 취소 / 환불 ──

    private int cancelCount;              // 취소 건수
    private int refundAmount;             // 환불 금액
    private int netSales;                 // 순매출 (총매출 - 환불)

    // ── 테이블 ──

    private int tableUseCount;            // 총 이용 횟수

    private int cardAmount;
    private int cardCount;

    private int transferAmount;
    private int transferCount;

    private int easyPayAmount;
    private int easyPayCount;

    private int phoneAmount;
    private int phoneCount;

    // ── 자식 엔티티 ──

    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @BatchSize(size = 50)
    private List<SettlementMenuRank> menuRanks = new ArrayList<>();

    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @BatchSize(size = 20)
    private List<SettlementCategoryRank> categoryRanks = new ArrayList<>();

    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @BatchSize(size = 30)
    private List<SettlementTableStat> tableStats = new ArrayList<>();

    // ── 편의 메서드 ──

    public void addMenuRank(SettlementMenuRank rank) {
        this.menuRanks.add(rank);
    }

    public void addCategoryRank(SettlementCategoryRank rank) {
        this.categoryRanks.add(rank);
    }

    public void addTableStat(SettlementTableStat stat) {
        this.tableStats.add(stat);
    }

    public void clearChildren() {
        this.menuRanks.clear();
        this.categoryRanks.clear();
        this.tableStats.clear();
    }

    public void update(
            int dayTotalAmount, int orderCount, int averageOrderAmount,
            int cancelCount, int refundAmount, int netSales,
            int cardAmount, int cardCount,
            int transferAmount, int transferCount,
            int easyPayAmount, int easyPayCount,
            int phoneAmount, int phoneCount,
            int tableUseCount
    ) {
        this.dayTotalAmount = dayTotalAmount;
        this.orderCount = orderCount;
        this.averageOrderAmount = averageOrderAmount;
        this.cancelCount = cancelCount;
        this.refundAmount = refundAmount;
        this.netSales = netSales;
        this.cardAmount = cardAmount;
        this.cardCount = cardCount;
        this.transferAmount = transferAmount;
        this.transferCount = transferCount;
        this.easyPayAmount = easyPayAmount;
        this.easyPayCount = easyPayCount;
        this.phoneAmount = phoneAmount;
        this.phoneCount = phoneCount;
        this.tableUseCount = tableUseCount;
    }
}