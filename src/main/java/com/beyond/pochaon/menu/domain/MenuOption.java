package com.beyond.pochaon.menu.domain;

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
public class MenuOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String optionName;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT), nullable = false)
    private Menu menu;
    @OneToMany(mappedBy = "menuOption", fetch = FetchType.LAZY,  cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MenuOptionDetail> menuOptionDetailList= new ArrayList<>();

    public void update(String optionName){
        this.optionName = optionName;
    }


//    메뉴 옵션상세 cascade필요시 사용
//    public void addMenuOptionDetail(MenuOptionDetail detail){
//        menuOptionDetailList.add(detail);
//        detail.setMenuOption(this);
//    }
//
//    public void removeMenuOptionDetail(MenuOptionDetail detail) {
//        menuOptionDetailList.remove(detail);
//        detail.setMenuOption(null);
//    }
}
