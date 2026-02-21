package com.beyond.pochaon.customerTable.dtos;

import com.beyond.pochaon.store.domain.Store;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class TableCreateReqDto {
    private int tableNum;


}
