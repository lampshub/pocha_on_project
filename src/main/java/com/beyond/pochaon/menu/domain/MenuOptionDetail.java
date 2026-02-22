package com.beyond.pochaon.menu.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuOptionDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String optionDetailName;
    private int optionDetailPrice;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_option_id", foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT), nullable = false)
    private MenuOption menuOption;

    public void update(String detailName, int detailPrice) {
        this.optionDetailName = detailName;
        this.optionDetailPrice = detailPrice;
    }
}

