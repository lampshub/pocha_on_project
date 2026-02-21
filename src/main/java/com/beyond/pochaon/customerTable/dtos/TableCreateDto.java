package com.beyond.pochaon.customerTable.dtos;

import com.beyond.pochaon.customerTable.domain.CustomerTable;
import com.beyond.pochaon.store.domain.Store;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class TableCreateDto {
    private int tableNum;
    private Store store;


}
