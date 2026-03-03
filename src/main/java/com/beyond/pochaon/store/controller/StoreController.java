package com.beyond.pochaon.store.controller;

import com.beyond.pochaon.store.dto.*;
import com.beyond.pochaon.store.service.StoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/store")
public class StoreController {

    private final StoreService storeService;

    @Autowired
    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createStore(@RequestBody StoreCreateDto dto, @AuthenticationPrincipal String email) {
        storeService.createStore(dto, email);
        return ResponseEntity.status(HttpStatus.CREATED).body("OK");
    }

    @PostMapping("/list")
    public ResponseEntity<?> findAll(@AuthenticationPrincipal String email) {
        List<StoreListDto> dtoList = storeService.findAll(email);
        return ResponseEntity.status(HttpStatus.OK).body(dtoList);
    }

    @PostMapping("/select")
    public StoreTokenDto selectStore(
            @RequestAttribute("email") String email,
            @RequestBody StoreSelectDto dto
    ) {
        return storeService.selectStore(email, dto.getStoreId());
    }

    @GetMapping("/{storeId}/time")
    public ResponseEntity<StoreTimeResDto> getStoreHours(@PathVariable Long storeId) {
        return ResponseEntity.ok(storeService.getStoreHours(storeId));
    }

    @PatchMapping("/{storeId}/updatetime")
    public ResponseEntity<?> updateTime(@PathVariable Long storeId, @RequestBody StoreUpdateTimeDto dto) {
        storeService.updateTime(storeId, dto);
        return ResponseEntity.status(HttpStatus.OK).body("운영시간이 수정되었습니다");
    }

//    매장 서비스이용기간 연장신청(authRenew = false인 매장만)
    @PostMapping("/{storeId}/updaterenewal")
    public ResponseEntity<?> updateRenewal(@PathVariable Long storeId,@AuthenticationPrincipal String email){
        storeService.updateRenewal(storeId, email);
        return ResponseEntity.status(HttpStatus.OK).body("서비스연장신청 완료");
    }

//    매장 서비스이용 자동연장 상태변경
    @PatchMapping("/{storeId}/updateAutoRenew")
    public ResponseEntity<?> updateAutoRenew(@PathVariable Long storeId, @AuthenticationPrincipal String email, Boolean autoRenew){
        storeService.updateAutoRenew(storeId, email, autoRenew);
        return ResponseEntity.status(HttpStatus.OK).body("서비스이용 자동연장 상태변경 완료");
    }

}