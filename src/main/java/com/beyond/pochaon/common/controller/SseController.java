package com.beyond.pochaon.common.controller;

import com.beyond.pochaon.common.auth.JwtTokenProvider;
import com.beyond.pochaon.common.repository.SseEmitterRegistry;
import com.beyond.pochaon.common.service.SseAlramService;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/sse")
@Slf4j
public class SseController {
    //데이터 형식
// {
//  "storeId": "1",
//  "tableNum": "3",
//  "message": "3번 테이블에서 직원을 호출했습니다."
//}
    private final SseEmitterRegistry sseEmitterRegistry;
    private final JwtTokenProvider jwtTokenProvider;
    private final SseAlramService sseAlramService;

    private final ScheduledExecutorService heartbeatScheduler =
            Executors.newScheduledThreadPool(2);

    @Autowired
    public SseController(SseEmitterRegistry sseEmitterRegistry, JwtTokenProvider jwtTokenProvider, SseAlramService sseAlramService) {
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.jwtTokenProvider = jwtTokenProvider;
        this.sseAlramService = sseAlramService;
    }

    //Sse연결 api 토큰의 stage를 통해 점주/테이블을 구분해서 각각의 Map에 등록
    @GetMapping("/connect")
    public SseEmitter connect(@RequestHeader("Authorization") String bearerToken) throws IOException {
        log.info("===connect start===");
        String token = bearerToken.substring(7);
        Claims claims = jwtTokenProvider.validateAccessToken(token);
        String stage = claims.get("stage", String.class);
        Long storeId = claims.get("storeId", Long.class);
        SseEmitter sseEmitter = new SseEmitter(20 * 60 * 60 * 1000L);
        if ("STORE".equals(stage)) {
            sseEmitterRegistry.addOwnerEmitter(String.valueOf(storeId), sseEmitter);
        } else if ("TABLE".equals(stage)) {
            Integer tableNum = jwtTokenProvider.getTableNum(token);
            sseEmitterRegistry.addSseEmitter(String.valueOf(storeId), String.valueOf(tableNum), sseEmitter);
        }
        sseEmitter.send(SseEmitter.event().name("connect").data("store 연결완료")); //연결될 때 나오는 로그
// heartbeat: 20초마다 comment 전송 → 45초 타임아웃 방지
        ScheduledFuture<?> heartbeat = heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                sseEmitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException | IllegalStateException e) {
                log.debug("heartbeat 전송 실패 - 연결 종료됨");
            }
        }, 20, 20, TimeUnit.SECONDS);

        // emitter 종료 시 heartbeat도 정리
        Runnable cleanup = () -> {
            heartbeat.cancel(false);
            if ("STORE".equals(false)) {
                sseEmitterRegistry.removeOwnerEmitter(String.valueOf(storeId), sseEmitter);
            } else if ("TABLE".equals(stage)) {
                int tableNum = jwtTokenProvider.getTableNum(token);
                sseEmitterRegistry.removeEmitter(String.valueOf(storeId), String.valueOf(tableNum));
            }
        };
            sseEmitter.onCompletion(cleanup);
            sseEmitter.onTimeout(cleanup);
            sseEmitter.onError(e -> cleanup.run());
            return sseEmitter;
    }

    @GetMapping("/disstaffcall")
    public void disconnect(@RequestHeader("Authorization") String bearerToken) {
        log.info("===disconnect===");
        String token = bearerToken.substring(7);
        Integer tableNum = jwtTokenProvider.getTableNum(token);
        Long storeId = jwtTokenProvider.getStoreId(token);
        sseEmitterRegistry.removeEmitter(String.valueOf(storeId), String.valueOf(tableNum));
    }


    //    테이블에서 직원호출 api
    @PostMapping("/staffcall")
    public ResponseEntity<?> callStaff(@RequestHeader("Authorization") String bearer) {
        String token = bearer.substring(7); //bearer 는 storeToken ?
        Long storeId = jwtTokenProvider.getStoreId(token);
        Integer tableNum = jwtTokenProvider.getTableNum(token);
        try {
            sseAlramService.sendToOwner(String.valueOf(storeId), String.valueOf(tableNum), tableNum + "번 테이블에서 직원을 호출했습니다.");
            return ResponseEntity.ok("호출완료");
        } catch (Exception e) {
            log.warn("직원 호출 전송 실패: {}", e.getMessage());
            return ResponseEntity.ok("호출 처리됨(재시도 예정)");
        }
    }
}
