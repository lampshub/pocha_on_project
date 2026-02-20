package com.beyond.pochaon.pay.controller;

import com.beyond.pochaon.pay.dto.KakaoPayCancelResponseDto;
import com.beyond.pochaon.pay.dto.QRPaymentResponseDto;
import com.beyond.pochaon.pay.service.KaKaoPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


// 프론트에서 /pay/kakao/ready?tableId=1 호출
// 응답의 qrcodeData를 qr이미지로 변환해서 화면에 표시

@RestController
@RequestMapping("/pay/kakao")
@Slf4j
public class KaKaoPayController {
    @Value("${client.domain}")
    private String clientDomain;

    private final KaKaoPayService kaKaoPayService;

    @Autowired
    public KaKaoPayController(KaKaoPayService kaKaoPayService) {
        this.kaKaoPayService = kaKaoPayService;
    }

    //    결제 준비 (qr생성) - 장바구니 기반
    @PostMapping("/ready")
    public ResponseEntity<QRPaymentResponseDto> readyPayment(@RequestParam Long tableId) {
        log.info("결제 준비: {}", tableId);
        QRPaymentResponseDto dto = kaKaoPayService.preparePayment(tableId);
        return ResponseEntity.ok(dto);
    }

    //   결제 승인 콜백 (카카오페이 -> 서버  -> 프론트로 )
    @GetMapping("/success")
    public ResponseEntity<?> approvePayment(@RequestParam("pg_token") String pgtToken, @RequestParam("partnerOrderId") String partnerOrderId) {

        try {
            log.info("결제 승인 콜백 - partnerOrderId: {}", partnerOrderId);
            kaKaoPayService.approvePayment(pgtToken, partnerOrderId);
            return ResponseEntity.status(HttpStatus.FOUND).header("Location", clientDomain + "/payment/success?orderId=" + partnerOrderId).build();
        } catch (Exception e) {
            log.error("결제 승인 실패", e);
            return ResponseEntity.status(HttpStatus.FOUND).header("Location", clientDomain + "/payment/fail?reason=" + e.getMessage()).build();
        }
    }

    //    결제 취소 콜백(사용자가 결제 화면에서 취소)
    @GetMapping("/cancel")
    public ResponseEntity<?> cancelCallBack() {
        log.warn("결제 취소 콜백");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("결제가 취소되었습니다.");
    }

    //    결제 실패 콜백
    @GetMapping("/fail")
    public ResponseEntity<?> failCallBack() {
        log.warn("결제 실패 콜백");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("결제에 실패했습니다");
    }

    //    점주 결제 취소(이미 승인된 결제에 한에서)
    @PostMapping("/cancel/{tid}")
    public ResponseEntity<KakaoPayCancelResponseDto> cancelPayment(@PathVariable String tid, @RequestParam(required = false, defaultValue = "사용자 요청") String cancelReason) {
        log.info("결제 취소 - Tid: {}, 사유: {}", tid, cancelReason);
        return ResponseEntity.ok(kaKaoPayService.cancelPayment(tid, cancelReason));
    }

    //   tid로 조회
    @GetMapping("/info/{tid}")
    public ResponseEntity<?> getPaymentInfo(@PathVariable String tid) {
        return ResponseEntity.ok(kaKaoPayService.getPaymentInfo(tid));
    }

    //    주문 번호로 조회
    @GetMapping("/order/{partnerOrderId}")
    public ResponseEntity<?> getPaymentInfoByOrderId(@PathVariable String partnerOrderId) {
        return ResponseEntity.ok(kaKaoPayService.getPaymentInfoByOrderId(partnerOrderId));
    }


}