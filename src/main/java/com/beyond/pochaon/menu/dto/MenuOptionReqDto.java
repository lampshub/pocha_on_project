package com.beyond.pochaon.menu.dto;

import com.beyond.pochaon.menu.domain.SelectionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
// 옵션 create, update 요청dto 같이 씀
public class MenuOptionReqDto {
    private String optionName;
    private SelectionType selectionType;
    private Integer minSelect;
    private Integer maxSelect;

}
