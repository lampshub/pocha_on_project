package com.beyond.pochaon.common.web;


import com.beyond.pochaon.customerorder.dto.OrderCreateDto;
import com.beyond.pochaon.present.dto.OwnerEventDto;
import com.beyond.pochaon.present.dto.PresentOwnerDto;
import com.beyond.pochaon.present.dto.PresentQueueDto;
import com.beyond.pochaon.present.dto.PresentReceiverDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
@Slf4j
@Component

public class WebSubscriber {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public WebSubscriber(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    public void onMessage(OwnerEventDto eventDto) {
// ->redis ->websocket
        log.info("[REDIS-SUB] Redis 채널 이벤트 수신 type={}, storeId={}", eventDto.getEventType(), eventDto.getStoreId());

        if("ORDER".equals(eventDto.getEventType())) {

            OrderCreateDto createDto = objectMapper.convertValue(eventDto.getPayload(), OrderCreateDto.class);
            messagingTemplate.convertAndSend("/topic/order/" + eventDto.getStoreId(), createDto);
            log.info("[~er/{}", eventDto.getStoreId());

        }else if("PRESENT".equals(eventDto.getEventType())){

            PresentOwnerDto presentOwnerDto = objectMapper.convertValue(eventDto.getPayload(),PresentOwnerDto.class);
            messagingTemplate.convertAndSend("/topic/order/"+ eventDto.getStoreId(), presentOwnerDto);
            log.info("[WS-PUBLISH] 점주선물주분발행 /topic/order/{}", eventDto.getStoreId());

        }else if("PRESENT_QUEUE".equals(eventDto.getEventType())){
            PresentQueueDto queueDto = objectMapper.convertValue(eventDto.getPayload(),PresentQueueDto.class);
            messagingTemplate.convertAndSend("topic/order-queue/" +eventDto.getStoreId());
            log.info("[WS-PUBLISH] 선물 큐 발행 /topic/order-queue/{}", eventDto.getStoreId());
        }
    }

    public void onTableMessage(PresentReceiverDto receiverDto){
        messagingTemplate.convertAndSend("/topic/table/"+receiverDto.getReceiverTableNum(), receiverDto);
    }
}
