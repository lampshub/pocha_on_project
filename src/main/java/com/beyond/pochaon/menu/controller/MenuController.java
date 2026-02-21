package com.beyond.pochaon.menu.controller;

import com.beyond.pochaon.menu.dtos.MenuCreateReqDto;
import com.beyond.pochaon.menu.dtos.MenuUpdateReqDto;
import com.beyond.pochaon.menu.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;

@RestController
@RequestMapping("/store/menu")
public class MenuController {
    private final MenuService menuService;
    @Autowired
    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }


    //    owner 메뉴 추가
    @PostMapping("/create")
    public ResponseEntity<?> createMenu(@ModelAttribute MenuCreateReqDto reqDto) throws AccessDeniedException {
        Long menuId = menuService.createMenu(reqDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(menuId);
    }

    //    owner 메뉴 수정
    @PutMapping("/{menuid}")
    public ResponseEntity<?> updateMenu(@PathVariable("menuid") Long menuId, @ModelAttribute MenuUpdateReqDto dto) throws AccessDeniedException {
        menuService.updateMenu(menuId, dto);
        return ResponseEntity.status(HttpStatus.OK).body("메뉴 수정 완료되었습니다");
    }

    //    owner 메뉴 삭제
    @DeleteMapping("/{menuid}")
    public ResponseEntity<?> deleteMenu(@PathVariable("menuid") Long menuId) throws AccessDeniedException {
        menuService.deleteMenu(menuId);
        return ResponseEntity.status(HttpStatus.OK).body("메뉴가 삭제되었습니다");
    }

    //    메뉴 상세 조회(점주 메뉴 수정시 기본값 세팅용)
    @GetMapping("/{menuid}/detail")
    public ResponseEntity<?> getMenuDetail(@PathVariable("menuid") Long menuId) throws AccessDeniedException {
        return ResponseEntity.ok(menuService.getMenuDetail(menuId));
    }
}
