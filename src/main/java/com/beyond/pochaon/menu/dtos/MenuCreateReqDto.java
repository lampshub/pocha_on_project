package com.beyond.pochaon.menu.dtos;

import com.beyond.pochaon.menu.domain.Category;
import com.beyond.pochaon.menu.domain.Menu;
import com.beyond.pochaon.store.domain.Store;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class MenuCreateReqDto {

    private String menuName;
    private int price;
    private String origin; //원산지
    private String explanation; //설명

    private Long categoryId;
    private MultipartFile menuImage;


    public Menu toEntity(Store store,Category category){
        return Menu.builder()
                .menuName(this.menuName)
                .price(this.price)
                .origin(this.origin)
                .explanation(this.explanation)
                .store(store)
                .category(category)
                .build();
    }
}