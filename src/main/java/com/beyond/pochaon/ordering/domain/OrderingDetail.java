package com.beyond.pochaon.ordering.domain;

import com.beyond.pochaon.menu.domain.Menu;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

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
    @BatchSize(size = 100)
    private List<OrderingDetailOption> orderingDetailOptions= new ArrayList<>();
}

