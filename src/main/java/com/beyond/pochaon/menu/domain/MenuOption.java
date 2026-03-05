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
    @Enumerated(EnumType.STRING)
    private SelectionType selectionType;
    private Integer minSelect;
    private Integer maxSelect;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT), nullable = false)
    private Menu menu;
    @OneToMany(mappedBy = "menuOption", fetch = FetchType.LAZY,  cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MenuOptionDetail> menuOptionDetailList= new ArrayList<>();

    public void update(String optionName, SelectionType selectionType, Integer maxSelect, Integer minSelect){
        this.optionName = optionName;
        this.selectionType = selectionType;
        this.minSelect =minSelect;
        this.maxSelect =minSelect;
    }

    @PrePersist
    public void setDefaults() {
        if(this.selectionType == SelectionType.SINGLE){
            this.minSelect = 1;
            this.maxSelect = 1;
        } else {
            if(this.minSelect == null) this.minSelect = 0;
            if(this.maxSelect == null) this.maxSelect = menuOptionDetailList.size();
        }
    }

}
