package com.beyond.pochaon.common.web;


import com.beyond.pochaon.customerorder.dto.OrderCreateDto;
import com.beyond.pochaon.present.dto.OwnerEventDto;
import com.beyond.pochaon.present.dto.PresentOwnerDto;
import com.beyond.pochaon.present.dto.PresentReceiverDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

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

        if("ORDER".equals(eventDto.getEventType())){
            OrderCreateDto createDto= objectMapper.convertValue(eventDto.getPayload(),OrderCreateDto.class);
            messagingTemplate.convertAndSend("/topic/order/" + eventDto.getStoreId(), createDto);

        }else if("PRESENT".equals(eventDto.getEventType())){
            PresentOwnerDto presentOwnerDto = objectMapper.convertValue(eventDto.getPayload(),PresentOwnerDto.class);
            messagingTemplate.convertAndSend("/topic/order/"+ eventDto.getStoreId(), presentOwnerDto);
    }
    }

    public void onTableMessage(PresentReceiverDto receiverDto){
        messagingTemplate.convertAndSend("/topic/table/"+receiverDto.getSenderTableNum(), receiverDto);
    }
}
