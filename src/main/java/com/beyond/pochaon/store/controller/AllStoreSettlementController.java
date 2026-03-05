package com.beyond.pochaon.store.controller;

import com.beyond.pochaon.store.dto.PeriodReqDto;
import com.beyond.pochaon.store.service.AllStoreSettlementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/store/settlement/all")
public class AllStoreSettlementController {

    private final AllStoreSettlementService allStoreSettlementService;

    @Autowired
    public AllStoreSettlementController(AllStoreSettlementService allStoreSettlementService) {
        this.allStoreSettlementService = allStoreSettlementService;
    }

    // 전체 매장 요약 (총 매출, 주문 수, 취소 수, 매장 수, 성장률, 객단가)
    @GetMapping("/summary")
    public ResponseEntity<?> getAllStoreSummary(
            @AuthenticationPrincipal String email,
            @ModelAttribute PeriodReqDto dto) {
        return ResponseEntity.ok(allStoreSettlementService.getAllStoreSummary(email, dto));
    }

    // 매장별 비교 (매출 랭킹, 성장률, 객단가)
    @GetMapping("/storecompare")
    public ResponseEntity<?> getStoreComparison(
            @AuthenticationPrincipal String email,
            @ModelAttribute PeriodReqDto dto) {
        return ResponseEntity.ok(allStoreSettlementService.getStoreComparison(email, dto));
    }

    // ★ 일별 상세 (달력 날짜 클릭 모달)
    @GetMapping("/day")
    public ResponseEntity<?> getAllStoreDay(
            @AuthenticationPrincipal String email,
            @ModelAttribute PeriodReqDto dto) {
        return ResponseEntity.ok(allStoreSettlementService.getAllStoreDay(email, dto));
    }

    // ★ 기간별 매출비교 (전체 매장 합산)
    @GetMapping("/salesanalysis")
    public ResponseEntity<?> getAllStoreSalesAnalysis(
            @AuthenticationPrincipal String email,
            @ModelAttribute PeriodReqDto dto) {
        return ResponseEntity.ok(allStoreSettlementService.getAllStoreSalesAnalysis(email, dto));
    }
}