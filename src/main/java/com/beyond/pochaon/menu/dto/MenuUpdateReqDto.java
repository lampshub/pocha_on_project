package com.beyond.pochaon.menu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class MenuUpdateReqDto {

    private String menuName;
    private int price;
    private String origin; //원산지
    private String explanation; //설명

    private Long categoryId;
    private MultipartFile menuImage;

    private Boolean deleteImage;

}