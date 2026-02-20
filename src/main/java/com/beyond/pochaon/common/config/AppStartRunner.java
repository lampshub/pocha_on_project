package com.beyond.pochaon.common.config;


import com.beyond.pochaon.owner.service.OwnerScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppStartRunner {
    private final OwnerScheduler ownerScheduler;

//    서버가 완전히 부팅되어 모든 준비가 끝난 직후에 이 메서드를 실행하게 함
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
//        서버가 재시작되어도 오늘 마감 예정인 매장들을 다시 예약함
        ownerScheduler.storeScheduler();
    }
}
