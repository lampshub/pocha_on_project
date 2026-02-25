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

    @GetMapping("/{storeid}/time")
    public ResponseEntity<StoreTimeResDto> getStoreHours(@PathVariable("storeid") Long storeId) {
        return ResponseEntity.ok(storeService.getStoreHours(storeId));
    }

    @PatchMapping("/{storeid}/updatetime")
    public ResponseEntity<?> updateTime(@PathVariable("storeid") Long storeId, @RequestBody StoreUpdateTimeDto dto) {
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

    // ── [2] 메뉴 분석 탭 ──

    @GetMapping("/analysis/menu")
    public ResponseEntity<MenuAnalysisResDto> getMenuAnalysis(
            @ModelAttribute MonthlySettlementReqDto dto,
            @RequestAttribute Long storeId) {
        return ResponseEntity.ok(storeService.getMenuAnalysis(storeId, dto.getYear(), dto.getMonth(), dto.getDay()));
    }

    // ── [3] 매출 분석 탭 ──

    @GetMapping("/analysis/sales")
    public ResponseEntity<SalesAnalysisResDto> getSalesAnalysis(
            @ModelAttribute MonthlySettlementReqDto dto,
            @RequestAttribute Long storeId) {
        return ResponseEntity.ok(storeService.getSalesAnalysis(storeId, dto.getYear(), dto.getMonth()));
    }

    // ── [4] 결제 분석 탭 ──

    @GetMapping("/analysis/payment")
    public ResponseEntity<PaymentAnalysisResDto> getPaymentAnalysis(
            @ModelAttribute MonthlySettlementReqDto dto,
            @RequestAttribute Long storeId) {
        return ResponseEntity.ok(storeService.getPaymentAnalysis(storeId, dto.getYear(), dto.getMonth()));
    }

    @GetMapping("/analysis/table")
    public ResponseEntity<TableAnalysisResDto> getTableAnalysis(
            @ModelAttribute MonthlySettlementReqDto dto,
            @RequestAttribute Long storeId) {
        return ResponseEntity.ok(storeService.getTableAnalysis(storeId, dto.getYear(), dto.getMonth()));
    }
}
