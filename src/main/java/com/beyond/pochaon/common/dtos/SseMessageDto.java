package com.beyond.pochaon.common.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseMessageDto {
    private String storeId;
    private String tableNum;
    private String message;
}
