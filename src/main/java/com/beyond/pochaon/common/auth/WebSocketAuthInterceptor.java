package com.beyond.pochaon.common.auth;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message, StompHeaderAccessor.class
        );

        if (accessor != null) {
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                handleConnect(accessor);
            }

            if (StompCommand.SEND.equals(accessor.getCommand())) {
                handleSend(accessor);
            }
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String authToken = accessor.getFirstNativeHeader("Authorization");

        if (authToken == null || !authToken.startsWith("Bearer ")) {
            log.warn(" WebSocket 연결: 토큰 없음");
            return;
        }

        String token = authToken.substring(7);

        try {
            Claims claims = jwtTokenProvider.validateAccessToken(token);

            String stage = claims.get("stage", String.class);
            String email = claims.getSubject();
            String role = claims.get("role", String.class);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );

            accessor.setUser(authentication);
            accessor.getSessionAttributes().put("stage", stage);
            accessor.getSessionAttributes().put("email", email);

            if ("STORE".equals(stage) || "TABLE".equals(stage)) {
                Long storeId = claims.get("storeId", Long.class);
                accessor.getSessionAttributes().put("storeId", storeId);
            }

            if ("TABLE".equals(stage)) {
                Integer tableNum = claims.get("tableNum", Integer.class);
                accessor.getSessionAttributes().put("tableNum", tableNum);
            }

            log.info(" WebSocket 인증 성공: {} (stage: {})", email, stage);

        } catch (Exception e) {
            log.warn(" WebSocket 토큰 검증 실패: {}", e.getMessage());
            throw new IllegalArgumentException("인증 실패");
        }
    }

    private void handleSend(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();

        if (destination != null && destination.startsWith("/app/chat")) {
            String stage = (String) accessor.getSessionAttributes().get("stage");

            if (!"TABLE".equals(stage)) {
                log.error(" 채팅 접근 거부: stage={}, destination={}", stage, destination);
                throw new IllegalArgumentException(
                        "채팅 기능은 TABLE stage 토큰이 필요합니다 (현재: " + stage + ")"
                );
            }

            log.debug(" 채팅 메시지 권한 확인 완료: {}", destination);
        }
    }
}