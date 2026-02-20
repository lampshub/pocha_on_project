package com.beyond.pochaon.menu.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
// 옵션상세 create, update 요청dto 같이 씀
public class MenuOptionDetailReqDto {

    private String optionDetailName;
    private int optionDetailPrice;
}
