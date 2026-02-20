package com.beyond.pochaon.common.repository;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseChatAlarmRegistry {
    private Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    public void addSseEmitter(String storeIdAndSenderTable, SseEmitter sseEmitter) throws IOException {
        this.emitterMap.put(storeIdAndSenderTable, sseEmitter);
        System.out.println(this.emitterMap.size());
        sseEmitter.onCompletion(() -> emitterMap.remove(storeIdAndSenderTable));        //창 닫을때 연결종료
        sseEmitter.onTimeout(() -> emitterMap.remove(storeIdAndSenderTable));           //1시간 설정 끝날때
        sseEmitter.onError(e -> emitterMap.remove(storeIdAndSenderTable));    //네트워크등 강제종료시 => 3개모두 콜백메서드(메모리누수 막음)
        System.out.println("현재 연결된 수: "+this.emitterMap.size());
    }

    public SseEmitter getEmitter(String storeIdAndReceiverTable){

        return this.emitterMap.get(storeIdAndReceiverTable);  //key값으로 get
    }

    public void removeEmitter(String storeIdAndSenderTable){
        this.emitterMap.remove(storeIdAndSenderTable);
        System.out.println("현재 연결된 수: "+this.emitterMap.size());

    }
}
