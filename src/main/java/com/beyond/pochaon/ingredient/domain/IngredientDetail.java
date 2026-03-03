package com.beyond.pochaon.ingredient.domain;

import com.beyond.pochaon.common.BaseTimeEntity;
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
public class IngredientDetail extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ingredient_detail_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(nullable = false)
    private int initialQuantity; // 입고 수량

    @Column(nullable = false)
    private int currentQuantity; // 현재 남은 수량

    @Column(nullable = false)
    private int unitPrice;       // 개당 단가

    @Column(nullable = false)
    private int totalPrice;      // 입고 총액

    private LocalDateTime deadline;  // 유통기한

    public void updateCurrentQuantity(int finalQuantity) {
        this.currentQuantity = finalQuantity;
    }
}
