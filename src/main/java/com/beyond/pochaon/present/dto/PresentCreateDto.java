package com.beyond.pochaon.present.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PresentCreateDto {
    private UUID idempotencyKey;

    private int senderTableNum;
    private int receiverTableNum;
    private Long menuId;
    private int menuQuantity;
}
