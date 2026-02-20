package com.beyond.pochaon.cart.controller;


import com.beyond.pochaon.cart.dto.cart_dto.CartCreateDto;
import com.beyond.pochaon.cart.dto.cart_dto.CartDto;
import com.beyond.pochaon.cart.dto.cart_dto.CartLineDeleteDto;
import com.beyond.pochaon.cart.dto.cart_dto.CartUpdateDto;
import com.beyond.pochaon.cart.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
public class CartController {
    private final CartService cartService;

    @Autowired
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }


    //    1.카트 생성
    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody CartCreateDto createDto, HttpServletRequest request) {
        Long tableId = (Long) request.getAttribute("tableId");
        if(tableId == null){
            throw new IllegalArgumentException("토큰이 필요합니다");
        }
        cartService.cartCreate(createDto,tableId);
        return ResponseEntity.status(HttpStatus.CREATED).body("ok");
    }


    //    2. 카트 조회
    @GetMapping("/list")
    public CartDto cartAll(HttpServletRequest request) {
        Long tableId = (Long) request.getAttribute("tableId");
        if(tableId == null){
            throw new IllegalArgumentException("토큰이 필요합니다");
        }
        CartDto cartDto = cartService.cartAll(tableId);
        return cartDto;
    }

    //    3. 카트 수정(수량변경)
    @PatchMapping("/quantity")
    public ResponseEntity<?> UpdateQuantity(@RequestBody CartUpdateDto updateDto,HttpServletRequest request) {
        Long tableId = (Long) request.getAttribute("tableId");
        if(tableId == null){
            throw new IllegalArgumentException("토큰이 필요합니다");
        }
        cartService.UpdateQuantity(updateDto,tableId);
        return ResponseEntity.status(HttpStatus.CREATED).body("ok");
    }

    //    4. 특정 줄 삭제
    @DeleteMapping("/line")
    public ResponseEntity<?> LineDelete(@RequestBody CartLineDeleteDto deleteDto,HttpServletRequest request) {
        Long tableId = (Long) request.getAttribute("tableId");
        if(tableId == null){
            throw new IllegalArgumentException("토큰이 필요합니다");
        }
        cartService.LineDelete(deleteDto,tableId);
        return ResponseEntity.status(HttpStatus.CREATED).body("ok");
    }

    //    5. 카트 비우기
    @DeleteMapping("/delete")
    public ResponseEntity<?> CartClear(HttpServletRequest request) {
        Long tableId = (Long) request.getAttribute("tableId");
        if(tableId == null){
            throw new IllegalArgumentException("토큰이 필요합니다");
        }
        cartService.CartClear(tableId);
        return ResponseEntity.status(HttpStatus.CREATED).body("ok");

    }
}
