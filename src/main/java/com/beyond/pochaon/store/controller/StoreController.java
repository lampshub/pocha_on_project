package com.beyond.pochaon.store.controller;

import com.beyond.pochaon.store.dtos.*;
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

    @PatchMapping("/{storeId}/updateTime")
    public ResponseEntity<?> updateTime(@PathVariable Long storeId, @RequestBody StoreUpdateTimeDto dto) {
        storeService.updateTime(storeId, dto);
        return ResponseEntity.status(HttpStatus.OK).body("운영시간이 수정되었습니다");
    }

    @GetMapping("/monthlysettlement")
    public ResponseEntity<?> getMonthlySettlement(@ModelAttribute MonthlySettlementReqDto dto, @RequestAttribute Long storeId) {
        return ResponseEntity.ok(storeService.getMonthlyCalender(storeId, dto.getYear(), dto.getMonth()));
    }

    @GetMapping("/dailysettlement")
    public ResponseEntity<?> getDailySettlement(@ModelAttribute DailySettlementReqDto dto, @RequestAttribute Long storeId) {
        return ResponseEntity.ok(storeService.getDailySettlement(storeId, dto.getYear(), dto.getMonth(), dto.getDay()));

    }


}
