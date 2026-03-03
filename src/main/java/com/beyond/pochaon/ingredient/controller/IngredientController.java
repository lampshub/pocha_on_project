package com.beyond.pochaon.ingredient.controller;

import com.beyond.pochaon.ingredient.dtos.IngredientMenuSaveReqDto;
import com.beyond.pochaon.ingredient.dtos.IngredientSaveReqDto;
import com.beyond.pochaon.ingredient.service.IngredientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ingredient")
public class IngredientController {
    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @PostMapping("/create")
    public ResponseEntity<String> ingredientSave(@RequestAttribute("storeId") Long storeId, @RequestBody IngredientSaveReqDto dto) {
        ingredientService.ingredientSave(storeId, dto);
        return ResponseEntity.ok("입고 처리가 완료되었습니다.");
    }

    @PostMapping("/recipe/create")
    public ResponseEntity<String> recipeSave(@RequestAttribute("storeId") Long storeId, @RequestBody IngredientMenuSaveReqDto dto) {
        ingredientService.recipeSave(storeId, dto);
        return ResponseEntity.ok("레시피 등록이 완료되었습니다.");
    }

    @DeleteMapping("/expired")
    public ResponseEntity<?> cleanupExpired(@RequestAttribute("storeId") Long storeId) {
        int count = ingredientService.cleanupExpiredIngredients(storeId);
        return ResponseEntity.ok("만료 재료 " + count + "건 정리 완료");
    }

}
