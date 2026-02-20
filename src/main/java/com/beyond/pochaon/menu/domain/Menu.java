package com.beyond.pochaon.menu.domain;


import com.beyond.pochaon.store.domain.Store;
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
public class Menu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String menuName;
    private int price;
    private String origin; //원산지
    private String explanation; //설명
    private String menuImageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id",foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT), nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id",foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT), nullable = false)
    private Store store;

    @OneToMany(mappedBy = "menu", fetch = FetchType.LAZY,  cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MenuOption> menuOptionList= new ArrayList<>();

    public void updateMenuImageUrl(String url){
        this.menuImageUrl = url;
    }

    public void update(String menuName, int price, String origin, String explanation, Category category) {
        this.menuName = menuName;
        this.price = price;
        this.origin = origin;
        this.explanation = explanation;
        this.category = category;
    }

}
