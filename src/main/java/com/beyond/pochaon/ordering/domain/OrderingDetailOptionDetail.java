package com.beyond.pochaon.ordering.domain;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderingDetailOptionDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String optionDetailName;

    private int optionDetailPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordering_detail_option_id", foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT), nullable = false)
    private OrderingDetailOption detailOption;
}
