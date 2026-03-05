package com.beyond.pochaon.store.controller;

import com.beyond.pochaon.store.dto.*;
import com.beyond.pochaon.store.service.StoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
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
        System.out.println(email);
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
        return storeService.selectStore(email, dto.getStoreId(), dto.getStoreAccessKey());
    }

    //    전체 정산에서 매장 선택했을 때 api
    @PostMapping("/select/settlement")
    public StoreTokenDto selectStoreForSettlement(
            @RequestAttribute("email") String email,
            @RequestBody StoreSelectDto dto
    ) {
        return storeService.selectStoreForSettlement(email, dto.getStoreId());
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

    //    매장 서비스이용 자동연장 상태변경
    @PatchMapping("/{storeId}/updateAutoRenew")
    public ResponseEntity<?> updateAutoRenew(@PathVariable Long storeId, @AuthenticationPrincipal String email, Boolean autoRenew) {
        storeService.updateAutoRenew(storeId, email, autoRenew);
        return ResponseEntity.status(HttpStatus.OK).body("서비스이용 자동연장 상태변경 완료");
    }

    @PostMapping("/islogin")
    public ResponseEntity<?> isLogin(@RequestBody List<IsLoginStoreReqDto> dtoList) {
        return ResponseEntity.status(HttpStatus.OK).body(storeService.isLogin(dtoList));
    }

    @DeleteMapping("/delete/{storeId}")
    public ResponseEntity<?> deletekey(@PathVariable("storeId") Long storeId) {
        storeService.deleteKey(storeId);
        return ResponseEntity.ok("OK");
    }

    //  매장 키 찾기: SMS 발송
    @PostMapping("/access-key/sms/send")
    public ResponseEntity<?> sendStoreAccessKeySms(
            @RequestBody StoreAccessKeyResetDto dto) {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        storeService.sendStoreAccessKeySms(email, dto.getStoreId());
        return ResponseEntity.ok().body("인증번호가 발송되었습니다.");
    }

    //  매장 키 재설정: SMS 검증 + 새 키 저장
    @PostMapping("/access-key/reset")
    public ResponseEntity<?> resetStoreAccessKey(
            @RequestBody StoreAccessKeyResetDto dto) {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        storeService.resetStoreAccessKey(
                email, dto.getStoreId(), dto.getCode(), dto.getNewAccessKey());
        return ResponseEntity.ok().body("매장 접근 키가 변경되었습니다.");
    }

}