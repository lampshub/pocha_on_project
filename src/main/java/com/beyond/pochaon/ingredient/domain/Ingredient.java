package com.beyond.pochaon.ingredient.domain;


import com.beyond.pochaon.common.BaseTimeEntity;
import com.beyond.pochaon.store.domain.Store;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Ingredient extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ingredient_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IngredientType type;

    @Column(nullable = false)
    private int safetyStock; // 안전재고

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String unit = "g"; // 측정 단위 (g, kg, ml, L, 개, 병, 팩 등)

    @OneToMany(mappedBy = "ingredient", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<IngredientDetail> details = new ArrayList<>();

    @OneToMany(mappedBy = "ingredient", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<IngredientMenu> ingredientMenus = new ArrayList<>();

    public void modifyName(String name) {
        this.name=name;
    }

    public void modifyType(IngredientType type) {
        this.type=type;
    }

    public void modifySafetyStock(int safetyStock) {
        this.safetyStock=safetyStock;
    }

    public void modifyUnit(String unit) {
        this.unit=unit;
    }
}
