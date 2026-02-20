package com.beyond.pochaon.pay.service;


import com.beyond.pochaon.pay.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@Transactional
@Slf4j
public class KaKaoPayApiService {

    private static final String BASE_URL = "https://open-api.kakaopay.com/online/v1/payment";

    private final RestTemplate restTemplate;

    @Value("${Kakao.pay.admin-key}")
    private String adminKey;

    @Value("${Kakao.pay.cid}")
    private String cid;

    @Autowired
    public KaKaoPayApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public KakaoPayReadyResponseDto ready(KakaoPayReadyRequestDto dto) {
        dto.setCid(cid);
        return callApi("/ready", dto, KakaoPayReadyResponseDto.class, "결제 준비");
    }

    public KakaoPayApproveResponseDto approve(KakaoPayApproveRequestDto dto) {
        dto.setCid(cid);
        return callApi("/approve", dto, KakaoPayApproveResponseDto.class, "결제 승인");
    }

    public KakaoPayCancelResponseDto cancel(KakaoPayCancelRequestDto dto) {
        dto.setCid(cid);
        return callApi("/cancel", dto, KakaoPayCancelResponseDto.class, "결제 취소");
    }

    //    공통 api 호출
    private <REQ, RES> RES callApi(String path, REQ request, Class<RES> responseType, String action) {
        try {
            HttpEntity<REQ> entity = new HttpEntity<>(request, createHeaders());
            log.info("카카오페이: {}, 요청: {} ", action, request);

            ResponseEntity<RES> response = restTemplate.postForEntity(BASE_URL + path, entity, responseType);
            RES body = response.getBody();
            log.info("카카오페이 {} , 응답: {}", action, body);
            return body;
        } catch (RestClientException e) {
            log.error("카카오페이 {} 실패", action, e);
            throw new RuntimeException("카카오ㅔ이 " + action + "중 오류 발생 ", e);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "DEV_SECRET_KEY " + adminKey);
        return headers;
    }

}
