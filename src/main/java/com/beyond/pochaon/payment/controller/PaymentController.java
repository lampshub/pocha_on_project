package com.beyond.pochaon.payment.controller;


import com.beyond.pochaon.payment.dto.PaymentDto;
import com.beyond.pochaon.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /*
    1. 주문 생성 (곃제 전 orderId 발급)
        프론트에서 결제 버튼 누르기 전에 호출하여 서버에 주문 정보를 저장하고 orderId를 발급 받음 -> 이걸로 토스 결제위젯에서 결제를 진행
     */
    @PostMapping("/order")
    public ResponseEntity<?> createOrder(@RequestBody PaymentDto.OrderCreateRequest request) {
        PaymentDto.OrderCreateResponse response = paymentService.createOrder(request);
        return ResponseEntity.ok(response);
    }

    //    결제 승인 (토스 -> 서버 -> 토스 api)
//    토스 결제위젯에서 결제 성공 후 리다이렉트 된 success 페이지에서 호출함
//      paymentKey, orderId, amount 를 받아 금액 검증 후 토스 api에 승인 요청
    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(@RequestBody PaymentDto.ConfirmRequest request) {
        PaymentDto.ConfirmResponse response = paymentService.confirmPayment(request);
        return ResponseEntity.ok(response);
    }

    //    결제 취소 필요시
    @PostMapping("/cancel")
    public ResponseEntity<Map<String, String>> cancelPayment(@RequestBody Map<String, String> request) {
        paymentService.cancelPayment(request.get("paymentKey"),
                request.getOrDefault("cancelReason", "고객 요청"));
        return ResponseEntity.ok(Map.of("message", "결제가 취소되었습니다"));
    }



}
