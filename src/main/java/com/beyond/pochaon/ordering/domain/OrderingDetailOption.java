package com.beyond.pochaon.ordering.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderingDetailOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderingOptionName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordering_detail_id", foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT), nullable = false)
    private OrderingDetail orderingDetail;

    @OneToMany(mappedBy = "detailOption", fetch = FetchType.LAZY,cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderingDetailOptionDetail> orderingDetailOptionDetails= new ArrayList<>();

}
