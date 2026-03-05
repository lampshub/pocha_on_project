package com.beyond.pochaon.present.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReactionDto {
    private Long storeId;
    private String reaction;
    private int senderTableNum;
    private int receiverTableNum;
}
