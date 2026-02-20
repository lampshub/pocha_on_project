package com.beyond.pochaon.menu.controller;

import com.beyond.pochaon.menu.dtos.CategoryViewDto;
import com.beyond.pochaon.menu.dtos.MenuDetailPageDto;
import com.beyond.pochaon.menu.dtos.MenuViewDto;
import com.beyond.pochaon.menu.service.ViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/view")
public class ViewController {

    private final ViewService viewService;
@Autowired
    public ViewController(ViewService viewService) {
        this.viewService = viewService;
    }

        // 1 전체 메뉴 조회
        @GetMapping("/all")
        public List<MenuViewDto> findAll() {
            return viewService.findAllMenu();
        }

        // 2 카테고리별 메뉴 조회
        @GetMapping("/category/{categoryId}")
        public CategoryViewDto Category(@PathVariable Long categoryId) {
            return viewService.findByCategory(categoryId);
        }

        // 3 메뉴 상세 조회
        @GetMapping("/{menuId}")
        public MenuDetailPageDto MenuDetail(@PathVariable Long menuId) {
            return viewService.findMenuDetail(menuId);
        }
    }


