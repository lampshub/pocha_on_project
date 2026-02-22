package com.beyond.pochaon.menu.dtos;

import com.beyond.pochaon.menu.domain.Category;
import com.beyond.pochaon.store.domain.Store;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryReqDto {
// 카테고리 create, update 요청dto 같이 씀
    private String categoryName;

    public Category toEntity(Store store) {
        return Category.builder()
                .categoryName(this.categoryName)
                .store(store)
                .build();
    }
}
