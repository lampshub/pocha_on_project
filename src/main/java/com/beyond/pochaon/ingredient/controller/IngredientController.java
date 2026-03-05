package com.beyond.pochaon.ingredient.controller;

import com.beyond.pochaon.ingredient.dtos.IngredientMenuSaveReqDto;
import com.beyond.pochaon.ingredient.dtos.IngredientSaveReqDto;
import com.beyond.pochaon.ingredient.dtos.IngredientUpdateReqDto;
import com.beyond.pochaon.ingredient.dtos.StockAdjustReqDto;
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

//    식자재 등록
    @PostMapping("/create")
    public ResponseEntity<String> ingredientSave(@RequestAttribute("storeId") Long storeId, @RequestBody IngredientSaveReqDto dto) {
        ingredientService.ingredientSave(storeId, dto);
        return ResponseEntity.ok("입고 처리가 완료되었습니다.");
    }

//    레시피(메뉴 당 사용 식자재 양) 등록
    @PostMapping("/recipe/create")
    public ResponseEntity<String> recipeSave(@RequestAttribute("storeId") Long storeId, @RequestBody IngredientMenuSaveReqDto dto) {
        ingredientService.recipeSave(storeId, dto);
        return ResponseEntity.ok("레시피 등록이 완료되었습니다.");
    }

//    유통기한 만료 재고 정리
    @DeleteMapping("/expired")
    public ResponseEntity<?> cleanupExpired(@RequestAttribute("storeId") Long storeId) {
        int count = ingredientService.cleanupExpiredIngredients(storeId);
        return ResponseEntity.ok("만료 재료 " + count + "건 정리 완료");
    }

    // 식자재 목록 조회
    @GetMapping("/list")
    public ResponseEntity<?> getIngredientList(@RequestAttribute("storeId") Long storeId) {
        return ResponseEntity.ok(ingredientService.getIngredientList(storeId));
    }

    // 특정 메뉴의 레시피 상세 조회
    @GetMapping("/recipe/{menuId}")
    public ResponseEntity<?> getRecipe(@PathVariable Long menuId) {
        return ResponseEntity.ok(ingredientService.getRecipeByMenu(menuId));
    }

    //  식자재 정보 수정
    @PatchMapping("/update/{ingredientId}")
    public ResponseEntity<String> updateIngredient(@PathVariable Long ingredientId, @RequestBody IngredientUpdateReqDto dto) {
        ingredientService.updateIngredient(ingredientId, dto);
        return ResponseEntity.ok("식자재 정보가 수정되었습니다.");
    }

    //  식자재 삭제
    @DeleteMapping("/{ingredientId}")
    public ResponseEntity<String> deleteIngredient(@PathVariable Long ingredientId) {
        ingredientService.deleteIngredient(ingredientId);
        return ResponseEntity.ok("식자재가 삭제되었습니다.");
    }

    //  실재고 수동 조정
    @PatchMapping("/adjust/{ingredientId}")
    public ResponseEntity<String> adjustStock(
            @PathVariable Long ingredientId,
            @RequestBody StockAdjustReqDto dto) {
        ingredientService.adjustStock(ingredientId, dto);
        return ResponseEntity.ok("실재고 조정 및 손실 기록이 완료되었습니다.");
    }
}
