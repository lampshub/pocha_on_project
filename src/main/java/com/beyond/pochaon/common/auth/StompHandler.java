package com.beyond.pochaon.common.auth;


import io.jsonwebtoken.Claims;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
public class StompHandler implements ChannelInterceptor {
    private final JwtTokenProvider jwtTokenProvider;

    public StompHandler(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        try {
//        jwt인증 (연결)

            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                String bearerToken = accessor.getFirstNativeHeader("Authorization");
                if (bearerToken == null || !bearerToken.startsWith("Bearer")) {
                    throw new IllegalArgumentException("토큰이 없습니다");
                }
                String token = bearerToken.substring(7);
                Claims claims = jwtTokenProvider.validateAccessToken(token);

//            stage값 검증(table, owner)
                String stage = claims.get("stage", String.class);
                String role = claims.get("role",String.class);

                if ("TABLE".equals(stage)) {
                    Number tableNum = claims.get("tableNum", Number.class);
                    if (tableNum == null) {
                        throw new IllegalArgumentException("토큰 내 존재하지 않는 table번호 입니다");
                    }
                    accessor.getSessionAttributes().put("stage", "TABLE");
                    accessor.getSessionAttributes().put("tableNum", tableNum.longValue());
                }else if("STORE".equals(stage) && "OWNER".equals(role)) {
                    Number storeId = claims.get("storeId", Number.class);
                    if (storeId == null) {
                        throw new IllegalArgumentException("토큰 내 존재하지 않는 storeId입니다");
                    }
                    accessor.getSessionAttributes().put("stage", "STORE");
                    accessor.getSessionAttributes().put("storeId", storeId.longValue());
                }else{
                    throw new IllegalArgumentException("허용되지 않은 사용자 입니다");
                }
            }


//            subscribe destination 권한검증
            if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                String destination = accessor.getDestination();
//              점주
                if (destination == null) {
                    throw new IllegalArgumentException("경로가 존재하지 않습니다");
                }
                String stage = (String) accessor.getSessionAttributes().get("stage");

                if (destination.startsWith("/topic/order/")) {
                    if (!"STORE".equals(stage)) {
                        throw new IllegalArgumentException("점주에게만 구독권한이 있습니다");
                    }
                    Long myStoreId = (Long) accessor.getSessionAttributes().get("storeId");
                    Long request = Long.parseLong(destination.replace("/topic/order/", ""));

                    if (!myStoreId.equals(request)) {
                        throw new IllegalArgumentException("해당 store만 구독이 가능합니다");
                    }
                }

//                테이블
                if (destination.startsWith("/topic/table/")) {
                    if (!"TABLE".equals(stage)) {
                        throw new IllegalArgumentException("테이블만 구독이 가능합니다");
                    }

                    Long myTableNum = (Long) accessor.getSessionAttributes().get("tableNum");
                    Long request = Long.parseLong(destination.replace("/topic/table/", ""));
                    if (!myTableNum.equals(request)) {
                        throw new IllegalArgumentException("해당하는 테이블만 구독이 가능합니다");
                    }
                }
            }

    } catch(Exception e){
            throw e;
        }
        return message;


    }
}

