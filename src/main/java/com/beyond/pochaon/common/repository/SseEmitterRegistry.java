package com.beyond.pochaon.common.repository;


import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseEmitterRegistry {

    private Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();
    private Map<String, SseEmitter> ownerEmitterMap = new ConcurrentHashMap<>();


//    테이블용 SseEmitter 키는 storeId:tableNum으로 저장
//    호출 시점 -> 테이블이 sse/connect로 연결할 때
    public void addSseEmitter(String storeId, String tableNum, SseEmitter sseEmitter) {
        String key = storeId + ":" + tableNum;
        this.emitterMap.put(key, sseEmitter);
        System.out.println(this.emitterMap.size());
    }

    public SseEmitter getEmitter(String storeId, String tableNum) {
        return this.emitterMap.get(storeId + ":" + tableNum);
    }

//    점주용: 해당 매장의 모든 emitter조회
//    매장 전체 테이블에 공지를 보낼 때 사용 (서비스 구현 전)
    public Map<String, SseEmitter> getEmitterByStore(String storeId) {
        Map<String, SseEmitter> result = new ConcurrentHashMap<>();
        emitterMap.forEach((key, emitter) -> {
            if (key.startsWith(storeId + ":")) {
                result.put(key, emitter);
            }
        });
        return result;
    }

//    테이블 emitter 제거용
    public void removeEmitter(String storeId, String tableNum) {
        String key = storeId + ":" + tableNum;
        this.emitterMap.remove(key);
        System.out.println(this.emitterMap.size());
    }

//    점주용 sseEmitter 등록
//    호출 시점 : 점주가 sse/connect로 연결할 때 == stage가 STORE일 때
    public void addOwnerEmitter(String storeId, SseEmitter sseEmitter) {
        ownerEmitterMap.put(storeId, sseEmitter);
    }

//    점주의 emitter 조회
//    호출 시점 : sendToOwner에서 점주에게 알림 보낼때
    public SseEmitter getOwnerEmitter(String storeId) {
        return ownerEmitterMap.get(storeId);
    }

//    점주 emitter 삭제
    public void removeOwnerEmitter(String storeId) {
        ownerEmitterMap.remove(storeId);
    }
}
