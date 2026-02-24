package com.beyond.pochaon.owner.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OwnerPasswordVerifyDto {
    private String password; // 프론트에서 입력한 평문 비밀번
    private Long customerTableId;
}
