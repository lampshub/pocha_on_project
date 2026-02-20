package com.beyond.pochaon.ordering.domain;

import com.beyond.pochaon.menu.domain.Menu;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderingDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int orderingDetailQuantity;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id",foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT), nullable = false)
    private Menu menu;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordering_id",foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT), nullable = false)
    private Ordering ordering;
    private int menuPrice;

    @OneToMany(mappedBy = "orderingDetail", fetch = FetchType.LAZY,cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderingDetailOption> orderingDetailOptions= new ArrayList<>();
}

