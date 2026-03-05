package com.beyond.pochaon.owner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OwnerSignUpSmsVerifyReqDto {
    private String phone;
    private String code;
}
