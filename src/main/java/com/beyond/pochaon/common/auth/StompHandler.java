package com.beyond.pochaon.common.auth;


import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
public class StompHandler implements ChannelInterceptor {
    private final JwtTokenProvider jwtTokenProvider;

    public StompHandler(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        // SUBSCRIBE 권한 검증만 담당
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination == null) {
                throw new IllegalArgumentException("경로가 존재하지 않습니다");
            }
            String stage = (String) accessor.getSessionAttributes().get("stage");

            if (destination.startsWith("/topic/order/") || destination.startsWith("/topic/order-queue/")) {
                if (!"STORE".equals(stage)) throw new IllegalArgumentException("점주에게만 구독권한이 있습니다");
                Long myStoreId = ((Number) accessor.getSessionAttributes().get("storeId")).longValue();
                String prefix = destination.startsWith("/topic/order-queue/") ? "/topic/order-queue/" : "/topic/order/";
                Long request = Long.parseLong(destination.replace(prefix, ""));
                if (!myStoreId.equals(request)) throw new IllegalArgumentException("해당 store만 구독이 가능합니다");
            }

            if (destination.startsWith("/topic/table/")) {
                if (!"TABLE".equals(stage)) throw new IllegalArgumentException("테이블만 구독이 가능합니다");
                Long myTableNum = ((Number) accessor.getSessionAttributes().get("tableNum")).longValue();
                Long request = Long.parseLong(destination.replace("/topic/table/", ""));
                if (!myTableNum.equals(request)) throw new IllegalArgumentException("해당하는 테이블만 구독이 가능합니다");
            }
        }

        return message;
    }
}

