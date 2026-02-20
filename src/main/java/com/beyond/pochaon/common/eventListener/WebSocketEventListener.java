package com.beyond.pochaon.common.eventListener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@Slf4j
public class WebSocketEventListener {

    @EventListener
    public void handleWebSocketDisconnectionListener(SessionDisconnectEvent event){
//        연결이 끊어질 때 로그아웃 처리나 같이 해주면 좋을 거 같습니다. (프론트에서도 작업 해줘야됨)
      log.info("연결이 끊어졌습니다,");

    }
}
