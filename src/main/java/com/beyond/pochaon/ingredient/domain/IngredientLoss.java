package com.beyond.pochaon.ingredient.domain;

import com.beyond.pochaon.common.BaseTimeEntity;
import com.beyond.pochaon.store.domain.Store;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class IngredientLoss extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ingredient_loss_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    private Long ingredientId; // 재료 Id

    @Column(nullable = false)
    private String ingredientName;  // 재료명 (삭제 후에도 이름 유지)

    private int lostQuantity;       // 폐기 수량
    private int unitPrice;          // 개당 단가
    private int lossAmount;         // 로스 금액 (단가 × 수량)
    private String reason;

    private LocalDateTime deadline; // 유통기한 (기록용)
}
