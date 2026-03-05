package com.beyond.pochaon.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreAccessKeyResetDto {
    private Long storeId;
    private String code;
    private String newAccessKey;
}
