package com.beyond.pochaon.store.domain;

import com.beyond.pochaon.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

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

    private int dayTotalAmount; //일일 매출
    private int orderCount;//오늘의 주문 횟수
    private int averageOrderAmount;//


    @ManyToOne
    @JoinColumn(nullable = false)
    private Store store;


}
