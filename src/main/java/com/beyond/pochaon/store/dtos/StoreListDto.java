package com.beyond.pochaon.store.dtos;

import com.beyond.pochaon.store.domain.Store;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreListDto {

    private Long id;
    private String StoreName;
    private LocalTime openAt;
    private LocalTime closedAt;
    private String address;

    public static StoreListDto fromEntity(Store store){
        return  StoreListDto.builder()
                .id(store.getId())
                .StoreName(store.getStoreName())
                .openAt(store.getStoreOpenAt())
                .closedAt(store.getStoreCloseAt())
                .address(store.getAddress())
                .build();
    }
}
