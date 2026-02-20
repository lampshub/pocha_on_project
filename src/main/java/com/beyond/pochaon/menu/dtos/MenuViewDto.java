package com.beyond.pochaon.menu.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class MenuViewDto {
    private Long menuId;
    private String menuName;
    private int menuPrice;
    private String imageUrl;



}
