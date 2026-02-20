package com.beyond.pochaon.menu.dtos;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CategoryViewDto {
    private Long categoryId;
    private String categoryName;
    private List<mappingMenu> mappingMenuList= new ArrayList<>();

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class mappingMenu {
        private Long menuId;
        private String menuName;
        private int menuPrice;
        private String imageUrl;


    }
}

