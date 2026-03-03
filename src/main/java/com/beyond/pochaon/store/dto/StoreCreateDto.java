package com.beyond.pochaon.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreCreateDto {

    private String storeName;
    private String address;
    private String phoneNumber;
    private LocalDate serviceStartAt;
    private boolean autoRenew;

}
