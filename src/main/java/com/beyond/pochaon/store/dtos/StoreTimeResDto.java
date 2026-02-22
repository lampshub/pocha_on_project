package com.beyond.pochaon.store.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class StoreTimeResDto {
    private LocalTime openAt;
    private LocalTime closeAt;
}
