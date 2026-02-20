package com.beyond.pochaon.present.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class OwnerEventDto {
    private String eventType;
    private Long storeId;
    private Object payload;

}
