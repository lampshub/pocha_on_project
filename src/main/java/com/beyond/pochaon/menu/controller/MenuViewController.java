package com.beyond.pochaon.menu.controller;

import com.beyond.pochaon.menu.dto.CategoryViewDto;
import com.beyond.pochaon.menu.dto.MenuDetailPageDto;
import com.beyond.pochaon.menu.dto.MenuViewDto;
import com.beyond.pochaon.menu.service.MenuViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/view")
public class MenuViewController {

    private final MenuViewService menuViewService;
@Autowired
    public MenuViewController(MenuViewService menuViewService) {
        this.menuViewService = menuViewService;
    }

        // 1 전체 메뉴 조회
        @GetMapping("/all")
        public List<MenuViewDto> findAll() throws AccessDeniedException {
            return menuViewService.findAllMenu();
        }

        // 2 카테고리별 메뉴 조회
        @GetMapping("/category/{categoryId}")
        public List<CategoryViewDto> Category(@PathVariable Long categoryId) throws AccessDeniedException {
            return menuViewService.findByCategory(categoryId);
        }

        // 3 메뉴 상세 조회
        @GetMapping("/{menuId}")
        public MenuDetailPageDto MenuDetail(@PathVariable Long menuId) throws AccessDeniedException {
            return menuViewService.findMenuDetail(menuId);
        }


    //        4.카테고리별 묶음
    @GetMapping("/category")
    public List<CategoryViewDto> getAllCategory() throws AccessDeniedException {
        return menuViewService.findAllCategory();
    }
}




