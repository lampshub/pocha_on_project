package com.beyond.pochaon.pay.service;


import com.beyond.pochaon.customerTable.domain.CustomerTable;
import com.beyond.pochaon.customerTable.repository.CustomerTableRepository;
import com.beyond.pochaon.ordering.domain.Ordering;
import com.beyond.pochaon.ordering.repository.OrderingRepository;
import com.beyond.pochaon.pay.domain.KakaoPay;
import com.beyond.pochaon.pay.domain.KakaoPayStatus;
import com.beyond.pochaon.pay.domain.PaymentState;
import com.beyond.pochaon.pay.dto.*;
import com.beyond.pochaon.pay.repository.KakaoPayRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 카카오페이 QR 결제 + 주문 생성 서비스
 * 플로우:
 * 1. preparePayment()  - 장바구니(Redis) 읽기 → 카카오 ready → QR URL 반환
 * 2. approvePayment()  - 카카오 approve → 주문 생성 → 장바구니 삭제 → WebSocket 알림
 * 3. cancelPayment()   - 카카오 cancel → 주문 상태 변경
 */

@Service
@Transactional
@Slf4j
public class KaKaoPayService {

    //   pay관련 객체 주입
    private final KaKaoPayApiService kaKaoPayApiService;
    private final KakaoPayRepository kakaoPayRepository;

    //    주문 생성에 필요한 repo
    private final OrderingRepository orderingRepository;
    private final CustomerTableRepository customerTableRepository;

    //   websocket 알림
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    //    서버 도메인
    @Value("${server.domain}")
    private String serverDomain;

    @Autowired
    public KaKaoPayService(KaKaoPayApiService kaKaoPayApiService, KakaoPayRepository kakaoPayRepository, OrderingRepository orderingRepository, CustomerTableRepository customerTableRepository, SimpMessagingTemplate messagingTemplate, @Qualifier("groupRedisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.kaKaoPayApiService = kaKaoPayApiService;
        this.kakaoPayRepository = kakaoPayRepository;
        this.orderingRepository = orderingRepository;
        this.customerTableRepository = customerTableRepository;
        this.messagingTemplate = messagingTemplate;
        this.redisTemplate = redisTemplate;
    }


    /*
    결제 준비 메서드(qr 생성)
     */
    //  1. 결제 준비 -> 장바구니 -> qr 생성
    public QRPaymentResponseDto preparePayment(Long tableId) {
//        테이블에서 groupId로 조회
        CustomerTable customerTable = customerTableRepository.findById(tableId).orElseThrow(() -> new EntityNotFoundException("테이블을 찾을 수 없습니다. kkpay_ser_prepare"));

        UUID groupId = customerTable.getGroupId();
        if (groupId == null) {
            throw new IllegalStateException("주문 내역이 없습니다. 먼저 주문해주세요/ kkpay_ser_prepare");
        }
//        groupId로 미결제(PENDING)상태 주문 조회
        List<Ordering> unpaidOrders = orderingRepository.findByGroupIdAndPaymentState(groupId, PaymentState.PENDING);

        if (unpaidOrders.isEmpty()) {
            throw new IllegalStateException("미결제 주문이 없습니다");
        }

//        총액 계산
        int totalAmount = unpaidOrders.stream().mapToInt(ordering -> ordering.getTotalPrice()).sum();

        if (totalAmount <= 0) {
            throw new IllegalStateException("결제금액이 0원 이하입니다. / kkpay_ser_prepare");
        }

//        상품명, 주문 ID
        String itemName = buildItemName(unpaidOrders);
        String orderingId = unpaidOrders.stream().map(o -> String.valueOf(o.getId())).reduce((a, b) -> a + "," + b).orElse("");

        String partnerOrderId = "ORDER_" + tableId + "_" + System.currentTimeMillis();
        String partnerUserId = "TABLE_" + tableId;

//        카카오페이 ready Api
        KakaoPayReadyRequestDto requestDto = KakaoPayReadyRequestDto.builder()
                .partnerOrderId(partnerOrderId)
                .partnerUserId(partnerUserId)
                .itemName(itemName)
                .quantity(unpaidOrders.size())
                .totalAmount(totalAmount)
                .taxFreeAmount(0)
                .approvalUrl(serverDomain + "/pay/kakao/success?partnerOrderId=" + partnerOrderId)
                .cancelUrl(serverDomain + "/pay/kakao/cancel")
                .failUrl(serverDomain + "/pay/kakao/fail")
                .build();

        KakaoPayReadyResponseDto readyResponseDto = kaKaoPayApiService.ready(requestDto);

//        db저장
        KakaoPay kakaoPay = KakaoPay.builder()
                .tid(readyResponseDto.getTid())
                .partnerOrderId(partnerOrderId)
                .partnerUserId(partnerUserId)
                .amount(totalAmount)
                .itemName(itemName)
                .quantity(unpaidOrders
                        .size())
                .tableId(tableId)
                .groupId(groupId)
                .orderingIds(orderingId)
                .status(KakaoPayStatus.READY)
                .nextRedirectAppUrl(readyResponseDto.getNextRedirectAppUrl())
                .nextRedirectPcUrl(readyResponseDto.getNextRedirectPcUrl())
                .nextRedirectMobileUrl(readyResponseDto.getNextRedirectMobileUrl())
                .build();
        kakaoPayRepository.save(kakaoPay);

        log.info("결제 준비 - TID: {}, TableId: {}, GroupId: {}, 금액: {}", readyResponseDto.getTid(), tableId, groupId, totalAmount);

        return QRPaymentResponseDto.builder()
                .tid(readyResponseDto.getTid())
                .nextRedirectAppUrl(readyResponseDto.getNextRedirectAppUrl())
                .nextRedirectPcUrl(readyResponseDto.getNextRedirectPcUrl())
                .nextRedirectMobileUrl(readyResponseDto.getNextRedirectMobileUrl())
                .qrCodeData(readyResponseDto.getNextRedirectMobileUrl())
                .createdAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .partnerOrderId(partnerOrderId)
                .amount(totalAmount)
                .itemName(itemName)
                .build();
    }

    /*
    결제 승인  메서드
     */
    public KakaoPayApproveResponseDto approvePayment(String pgToken, String partnerOrderId) {

//      결제하고 난 뒤 결제 정보 조회
        KakaoPay kakaoPay = kakaoPayRepository.findByPartnerOrderIdAndStatus(partnerOrderId, KakaoPayStatus.READY).orElseThrow(() -> new EntityNotFoundException("결제 정보를 찾을 수 없음,kkpay_ser_approve"));

//        카카오페이 승인 api
        KakaoPayApproveRequestDto approveRequest = KakaoPayApproveRequestDto.builder()
                .tid(kakaoPay.getTid())
                .partnerOrderId(kakaoPay.getPartnerOrderId())
                .partnerUserId(kakaoPay.getPartnerUserId())
                .pgToken(pgToken)
                .build();
        KakaoPayApproveResponseDto approveResponse = kaKaoPayApiService.approve(approveRequest);
//        결제 상태 업데이트
        LocalDateTime approveAt = LocalDateTime.parse(approveResponse.getApprovedAt(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        kakaoPay.approve(approveResponse.getAid(), approveAt);
//        주문들 completed로 변경
        updateOrdersPaymentState(kakaoPay.getOrderingIds(), PaymentState.COMPLETED);

//        테이블 정리
        Long tableId = kakaoPay.getTableId();
        CustomerTable table = customerTableRepository.findById(tableId).orElseThrow(() -> new EntityNotFoundException("테이블 없음"));

//        redis그룹 Id 삭제
        redisTemplate.delete(String.valueOf(table.getTableNum()));
//        테이블 초기화
        table.clearTable();
//      점주화면 알림
        Long storeId = table.getStore().getId();
        messagingTemplate.convertAndSend(
                "/topic/table-status/" + storeId,
                Map.of(
                        "type", "TABLE_CLEARED",
                        "tableId", tableId,
                        "tableNum", table.getTableNum(),
                        "status", "STANDBY"
                )
        );

//         웹소켓 알림(손님이 결제 완료했을 때)
        sendPaymentNotification(kakaoPay, PaymentState.COMPLETED.name());
        log.info("결제 승인 - TID: {}, 금액: {} ", kakaoPay.getTid(), kakaoPay.getAmount());
        return approveResponse;
    }

    /*
    결제 취소 메서드
     */
    public KakaoPayCancelResponseDto cancelPayment(String tid, String cancelReason) {
        KakaoPay kakaoPay = kakaoPayRepository.findByTid(tid).orElseThrow(() -> new EntityNotFoundException("결제내역을 찾을 수 없습니다, kkpay_ser_cancel"));

        if (kakaoPay.getStatus() != KakaoPayStatus.APPROVED) {
            throw new IllegalStateException("승인된 결제만 취소 가능.kkpay_ser_cancel 현재: " + kakaoPay.getStatus());
        }

        KakaoPayCancelRequestDto cancelRequest = KakaoPayCancelRequestDto.builder()
                .tid(tid)
                .cancelAmount(kakaoPay.getAmount())
                .cancelTaxFreeAmount(0)
                .cancelReason(cancelReason)
                .build();
        KakaoPayCancelResponseDto cancelResponse = kaKaoPayApiService.cancel(cancelRequest);

        LocalDateTime canceledAt = LocalDateTime.parse(cancelResponse.getCanceledAt(),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        kakaoPay.cancel(kakaoPay.getAmount(), cancelReason, canceledAt);

//     주문들 CANCELED로 변경
        updateOrdersPaymentState(kakaoPay.getOrderingIds(), PaymentState.CANCELED);
        log.info("결제 취소- TID: {}", tid);
        return cancelResponse;
    }

    /*
    조회===========================
     */
    @Transactional(readOnly = true)
    public KakaoPay getPaymentInfo(String tid) {
        return kakaoPayRepository.findByTid(tid).orElseThrow(() -> new EntityNotFoundException("결제 정보를 찾을 수 없습니다 kakapay_ser_getPaymentInfo"));
    }

    @Transactional(readOnly = true)
    public KakaoPay getPaymentInfoByOrderId(String partnerOrderId) {
        return kakaoPayRepository.findByPartnerOrderId(partnerOrderId).orElseThrow(() -> new EntityNotFoundException("결제 정보를 찾을 수 없음, kakapay_ser_getPaymentInfoByOrderId"));
    }

    /*

    아래는 유틸 메서드
     */

    //  장바구니 첫 메뉴명으로 짜장면 외 2건 형태로 만들어줌 //신규
    private String buildItemName(List<Ordering> ordering) {
        if (ordering.isEmpty()) return "주문 상품";
        Ordering first = ordering.get(0);
        if (first.getOrderDetail().isEmpty()) return "주문 상품";
        String firstName = first.getOrderDetail().get(0).getMenu().getMenuName();
        int total = ordering.stream().mapToInt(o -> o.getOrderDetail().size()).sum();
        return total == 1 ? firstName : firstName + " 외 " + (total - 1) + "건";
    }

    //    신규 해당 결제 주문들 상태 바꿔줌
    private void updateOrdersPaymentState(String orderingIds, PaymentState state) {
        if (orderingIds == null || orderingIds.isEmpty()) return;

        for (String id : orderingIds.split(",")) {
            Long orderingId = Long.parseLong(id.trim());
            orderingRepository.findById(orderingId).ifPresent(ordering -> ordering.updatePaymentState(state));
        }
    }

    //    신규
//    결제가 일어난 테이블의 매장ID를 찾아 그 매장의 관리자 페이지(웹소켓 구독중)로 결제 상태와 금액을 실시간 전송하는 콛
    private void sendPaymentNotification(KakaoPay kakaoPay, String status) {
        customerTableRepository.findById(kakaoPay.getTableId()).ifPresent(table -> {
            Long storeId = table.getStore().getId();
            messagingTemplate.convertAndSend( //웹소켓 pub 역할
                    "/topic/table-status" + storeId, //해당 매장의 고유 채널
//                    키:밸류값으로 한번에 만든 코드
                    Map.of("tableId", kakaoPay.getTableId(),
                            "status", status,
                            "amount", kakaoPay.getAmount())
            );
        });
    }
}