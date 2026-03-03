package com.beyond.pochaon.customerTable.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class TableStatusEventDto {
    private int tableNum;
    private String status; // "USING" | "AVAILABLE"
}
