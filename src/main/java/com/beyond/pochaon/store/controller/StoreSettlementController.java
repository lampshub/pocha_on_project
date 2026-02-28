package com.beyond.pochaon.store.controller;

import com.beyond.pochaon.store.service.StoreSettlementService;
import com.beyond.pochaon.store.settlementdto.PeriodReqDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/store/settlement")
public class StoreSettlementController {
    private final StoreSettlementService storeSettlementService;

    @Autowired
    public StoreSettlementController(StoreSettlementService storeSettlementService) {
        this.storeSettlementService = storeSettlementService;
    }

    //일별 정산
    @GetMapping("/daily")
    public ResponseEntity<?> getDaily(@RequestAttribute Long storeId, @ModelAttribute PeriodReqDto dto) {
        return ResponseEntity.ok(storeSettlementService.getDailySettlement(storeId, dto));
    }

    //    주별 정산
    @GetMapping("/weekly")
    public ResponseEntity<?> getWeekly(@RequestAttribute Long storeId, @ModelAttribute PeriodReqDto dto) {
        return ResponseEntity.ok(storeSettlementService.getWeeklySettlement(storeId, dto));
    }

    //    월별 정산
    @GetMapping("/monthly")
    public ResponseEntity<?> getMonthly(@RequestAttribute Long storeId,
                                        @ModelAttribute PeriodReqDto dto) {
        return ResponseEntity.ok(storeSettlementService.getMonthlySettlement(storeId, dto));
    }

//    메뉴 분석
    @GetMapping("/menuanalysis")
    public ResponseEntity<?> getMenuAnalysis(@RequestAttribute Long storeId,
                                             @ModelAttribute PeriodReqDto dto) {
        return ResponseEntity.ok(storeSettlementService.getMenuAnalysis(storeId, dto));
    }
    // 매출 분석
    @GetMapping("/salesanalysis")
    public ResponseEntity<?> getSalesAnalysis(
            @RequestAttribute Long storeId,
            @ModelAttribute PeriodReqDto dto
    ) {
        return ResponseEntity.ok(storeSettlementService.getSalesAnalysis(storeId, dto));
    }

    // 결제 분석
    @GetMapping("/paymentanalysis")
    public ResponseEntity<?> getPaymentAnalysis(
            @RequestAttribute Long storeId,
            @ModelAttribute PeriodReqDto dto
    ) {
        return ResponseEntity.ok(storeSettlementService.getPaymentAnalysis(storeId, dto));
    }

    // 테이블 분석
    @GetMapping("/tableanalysis")
    public ResponseEntity<?> getTableAnalysis(
            @RequestAttribute Long storeId,
            @ModelAttribute PeriodReqDto dto
    ) {
        return ResponseEntity.ok(storeSettlementService.getTableAnalysis(storeId, dto));
    }

}
