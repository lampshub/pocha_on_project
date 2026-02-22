package com.beyond.pochaon.menu.controller;

import com.beyond.pochaon.menu.dtos.CategoryReqDto;
import com.beyond.pochaon.menu.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;

@RestController
@RequestMapping("/store/category")
public class CategoryController {
    private final CategoryService categoryService;
    @Autowired
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    //    메뉴 카테고리 추가
    @PostMapping("/create")
    public ResponseEntity<?> createCategory(@RequestBody CategoryReqDto reqDto) throws AccessDeniedException {
        categoryService.createCategory(reqDto);
        return ResponseEntity.status(HttpStatus.CREATED).body("카테고리 추가 완료");
    }

    //    메뉴 카테고리 수정
    @PutMapping("/{categoryid}")
    public ResponseEntity<?> updateCategory(@PathVariable("categoryid") Long categoryId, @RequestBody CategoryReqDto reqDto) throws AccessDeniedException {
        categoryService.updateCategory(categoryId, reqDto);
        return ResponseEntity.status(HttpStatus.OK).body("카테고리 수정 완료");
    }

    //    메뉴 카테고리 삭제
    @DeleteMapping("/{categoryid}")
    public ResponseEntity<?> deleteCategory(@PathVariable("categoryid") Long categoryId) throws AccessDeniedException {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.status(HttpStatus.OK).body("카테고리 삭제 완료");
    }
}
