package com.beyond.pochaon.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SmsVerifyReqDto {
    private String name;
    private String phone;
    private String code;
}
