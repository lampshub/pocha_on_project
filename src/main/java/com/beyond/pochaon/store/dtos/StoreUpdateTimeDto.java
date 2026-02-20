package com.beyond.pochaon.store.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreUpdateTimeDto {

    private LocalTime openAt;   // 00:00 또는 00:00:00 형식으로 입력
    private LocalTime closeAt;

}
