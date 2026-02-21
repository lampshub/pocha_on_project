package com.beyond.pochaon.menu.controller;

import com.beyond.pochaon.menu.dtos.MenuOptionReqDto;
import com.beyond.pochaon.menu.service.MenuOptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;

@RestController
@RequestMapping("/store/menu")
public class MenuOptionController {
    private final MenuOptionService menuOptionService;
    @Autowired
    public MenuOptionController(MenuOptionService menuOptionService) {
        this.menuOptionService = menuOptionService;
    }


    //    owner 메뉴옵션 추가
    @PostMapping("/{menuid}/option")
    public ResponseEntity<?> createOption(@PathVariable("menuid") Long menuId, @ModelAttribute  MenuOptionReqDto reqDto) throws AccessDeniedException {
        return ResponseEntity.status(HttpStatus.CREATED).body(menuOptionService.createOption(menuId, reqDto));
    }

    //    owner 메뉴옵션 수정
    @PutMapping("/option/{optionid}")
    public ResponseEntity<?> updateOption(@PathVariable("optionid") Long optionId, @ModelAttribute  MenuOptionReqDto reqDto) throws AccessDeniedException {
        menuOptionService.updateOption(optionId, reqDto);
        return ResponseEntity.status(HttpStatus.OK).body("메뉴옵션 수정 완료되었습니다");
    }

    // owner 메뉴옵션 삭제
    @DeleteMapping("/option/{optionid}")
    public ResponseEntity<?> deleteOption(@PathVariable("optionid") Long optionId) throws AccessDeniedException {
        menuOptionService.deleteOption(optionId);
        return ResponseEntity.status(HttpStatus.OK).body("메뉴옵션이 삭제되었습니다");
    }
}
