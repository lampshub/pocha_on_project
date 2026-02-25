package com.beyond.pochaon.payment.service;

import com.beyond.pochaon.ordering.domain.Ordering;
import com.beyond.pochaon.ordering.repository.OrderingRepository;
import com.beyond.pochaon.payment.dto.PaymentDto.ConfirmRequest;
import com.beyond.pochaon.payment.dto.PaymentDto.ConfirmResponse;
import com.beyond.pochaon.payment.dto.PaymentDto.OrderCreateRequest;
import com.beyond.pochaon.payment.dto.PaymentDto.OrderCreateResponse;
import com.beyond.pochaon.payment.entity.PayerType;
import com.beyond.pochaon.payment.entity.Payment;
import com.beyond.pochaon.payment.entity.PaymentStatus;
import com.beyond.pochaon.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final OrderingRepository orderingRepository;

    // application.yml에서 주입
    @Value("${toss.payments.secret-key}")
    private String secretKey;

    private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";
    private static final String TOSS_CANCEL_URL = "https://api.tosspayments.com/v1/payments/{paymentKey}/cancel";

    public PaymentService(PaymentRepository paymentRepository, RestTemplate restTemplate, ObjectMapper objectMapper, @Qualifier("groupRedisTemplate") RedisTemplate<String, String> redisTemplate, OrderingRepository orderingRepository) {
        this.paymentRepository = paymentRepository;
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.orderingRepository = orderingRepository;
    }

    // ─── 1. 주문 생성 (결제 전 orderId 발급) ──────────────────

    public OrderCreateResponse createOrder(OrderCreateRequest request) {

        // orderId 생성: UUID 기반 (토스페이먼츠 권장: 영문 대소문자 + 숫자 + 하이픈/언더바, 6~64자)
        String orderId = "POCHA_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);

        // PayerType 변환
        PayerType payerType = PayerType.CUSTOMER;
        if ("POS".equalsIgnoreCase(request.getPayerType())) {
            payerType = PayerType.POS;
        }

        Payment payment = Payment.builder()
                .orderId(orderId)
                .orderName(request.getOrderName())
                .amount(request.getAmount())
                .status(PaymentStatus.READY)
                .payerType(payerType)
                .tableNum(request.getTableNum())
                .storeId(request.getStoreId())
                .groupId(request.getGroupId())
                .build();

        paymentRepository.save(payment);

        log.info("[Payment] 주문 생성 - orderId: {}, amount: {}, table: {}",
                orderId, request.getAmount(), request.getTableNum());

        return OrderCreateResponse.builder()
                .orderId(orderId)
                .build();
    }

    // ─── 2. 결제 승인 (토스페이먼츠 API 호출) ─────────────────

    public ConfirmResponse confirmPayment(ConfirmRequest request) {

        // DB에서 주문 조회
        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다: " + request.getOrderId()));

        // 금액 검증 (프론트에서 조작 방지)
        if (payment.getAmount() != request.getAmount()) {
            payment.fail();
            paymentRepository.save(payment);
            throw new IllegalArgumentException(
                    String.format("결제 금액이 일치하지 않습니다. (서버: %d, 요청: %d)",
                            payment.getAmount(), request.getAmount()));
        }

        // 이미 처리된 결제인지 확인
        if (payment.getStatus() != PaymentStatus.READY) {
            throw new IllegalStateException("이미 처리된 결제입니다. 현재 상태: " + payment.getStatus());
        }

        // 토스페이먼츠 결제 승인 API 호출
        try {
            HttpHeaders headers = createTossHeaders();

            String body = objectMapper.writeValueAsString(request);

            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    TOSS_CONFIRM_URL, HttpMethod.POST, entity, String.class);

            // 응답 파싱
            JsonNode responseBody = objectMapper.readTree(response.getBody());

            String paymentKey = responseBody.get("paymentKey").asText();
            String method = responseBody.has("method") ? responseBody.get("method").asText() : null;
            String approvedAtStr = responseBody.has("approvedAt") ? responseBody.get("approvedAt").asText() : null;

            LocalDateTime approvedAt = null;
            if (approvedAtStr != null) {
                approvedAt = LocalDateTime.parse(approvedAtStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            }

            // DB 업데이트
            payment.approve(paymentKey, method, approvedAt);
            paymentRepository.save(payment);
            List<Ordering> orderings = orderingRepository.findByGroupId(UUID.fromString(payment.getGroupId()));
            for (Ordering ordering : orderings) {
                ordering.updatePaymentState(PaymentStatus.DONE);
            }

            log.info("[Payment] 결제 승인 완료 - orderId: {}, paymentKey: {}, method: {}",
                    payment.getOrderId(), paymentKey, method);

            String groupKey = String.valueOf(payment.getTableNum());
            redisTemplate.delete(groupKey);

            return ConfirmResponse.builder()
                    .paymentKey(paymentKey)
                    .orderId(payment.getOrderId())
                    .orderName(payment.getOrderName())
                    .totalAmount(payment.getAmount())
                    .method(method)
                    .status(PaymentStatus.DONE)
                    .approvedAt(approvedAtStr)
                    .build();

        } catch (HttpClientErrorException e) {
            // 토스 API 에러 처리
            log.error("[Payment] 토스 결제 승인 실패 - orderId: {}, response: {}",
                    request.getOrderId(), e.getResponseBodyAsString());

            payment.fail();
            paymentRepository.save(payment);

            try {
                JsonNode errorBody = objectMapper.readTree(e.getResponseBodyAsString());
                String errorCode = errorBody.has("code") ? errorBody.get("code").asText() : "UNKNOWN";
                String errorMsg = errorBody.has("message") ? errorBody.get("message").asText() : "결제 승인에 실패했습니다";
                throw new RuntimeException(errorCode + ": " + errorMsg);
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception parseEx) {
                throw new RuntimeException("결제 승인에 실패했습니다");
            }

        } catch (Exception e) {
            payment.fail();
            paymentRepository.save(payment);
            log.error("[Payment] 결제 승인 중 예외 발생", e);
            throw new RuntimeException("결제 승인 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // ─── 3. 결제 취소 (필요 시 사용) ──────────────────────────

    public void cancelPayment(String paymentKey, String cancelReason) {

        Payment payment = paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결제입니다"));

        try {
            HttpHeaders headers = createTossHeaders();

            String body = objectMapper.writeValueAsString(
                    java.util.Map.of("cancelReason", cancelReason));

            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            String url = TOSS_CANCEL_URL.replace("{paymentKey}", paymentKey);
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            payment.cancel();
            paymentRepository.save(payment);

            log.info("[Payment] 결제 취소 완료 - paymentKey: {}", paymentKey);

        } catch (Exception e) {
            log.error("[Payment] 결제 취소 실패 - paymentKey: {}", paymentKey, e);
            throw new RuntimeException("결제 취소에 실패했습니다: " + e.getMessage());
        }
    }

    // ─── Authorization 헤더 생성 ─────────────────────────────

    private HttpHeaders createTossHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Basic 인증: secretKey + ":" 를 Base64 인코딩
        String encodedKey = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedKey);

        return headers;
    }
}