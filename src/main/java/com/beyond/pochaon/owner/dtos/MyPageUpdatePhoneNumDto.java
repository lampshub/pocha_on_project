package com.beyond.pochaon.owner.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class MyPageUpdatePhoneNumDto {
    private String phoneNumber;
}
