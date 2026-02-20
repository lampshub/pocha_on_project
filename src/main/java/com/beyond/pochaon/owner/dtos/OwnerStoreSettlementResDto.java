package com.beyond.pochaon.owner.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OwnerStoreSettlementResDto {
    private int storeCount; //가게 개수
    private String topStoreName; //매출이 가장 높은 가게이름
    private int storeTotalAmount; //가게 일일 모든 매출
    private int storeDayAverageAmount; //모든 가게에 일일 평균매출
}
