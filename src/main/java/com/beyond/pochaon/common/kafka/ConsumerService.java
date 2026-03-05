package com.beyond.pochaon.common.kafka;

import com.beyond.pochaon.menu.domain.OrderAlarmTo;
import com.beyond.pochaon.ordering.dto.OrderCreateDto;
import com.beyond.pochaon.ordering.dto.OrderQueueDto;
import com.beyond.pochaon.owner.domain.Owner;
import com.beyond.pochaon.present.dto.EventQueDto;
import com.beyond.pochaon.present.dto.OwnerEventDto;
import com.beyond.pochaon.present.dto.PresentOwnerDto;
import com.beyond.pochaon.present.dto.PresentQueueDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ConsumerService {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;


    public ConsumerService(ObjectMapper objectMapper, SimpMessagingTemplate messagingTemplate) {
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
    }
//     주문1. 일반 주문 분기 (점주)
@KafkaListener(topics = "order-topic", groupId = "owner-topic-group", containerFactory = "OwnerListener") //다른 서버 가정
public void owner(@Header(KafkaHeaders.RECEIVED_KEY) String key, String message) throws JsonProcessingException {
    OwnerEventDto ownerEventDto = objectMapper.readValue(message, OwnerEventDto.class);
//      Que주문정보 분기(일반/선물)
    if ("PRESENT".equals(ownerEventDto.getEventType())) {
        PresentOwnerDto presentOwnerDto = objectMapper.convertValue(ownerEventDto.getPayload(), PresentOwnerDto.class);
        messagingTemplate.convertAndSend("/topic/owner/" + key, presentOwnerDto);
    } else {
        OrderCreateDto orderCreateDto = objectMapper.convertValue(ownerEventDto.getPayload(), OrderCreateDto.class);
        messagingTemplate.convertAndSend("/topic/owner/" + key, orderCreateDto);
    }
}



//    주문2. Que -같은 토픽, 다른 그룹 아이디 -> 점주/주방-일반/선물 분기

    @KafkaListener(topics = "orderQue-topic", groupId = "ownerQue-topic-group", containerFactory = "OwnerListener") //다른 서버 가정
    public void ownerQue(@Header(KafkaHeaders.RECEIVED_KEY) String key, String message) throws JsonProcessingException {
        EventQueDto eventQueDto = objectMapper.readValue(message, EventQueDto.class);
//      Que주문정보 분기(일반/선물)
        if ("PRESENT".equals(eventQueDto.getEventType())) {
            PresentQueueDto presentQueueDto = objectMapper.convertValue(eventQueDto.getPayload(), PresentQueueDto.class);
            //        카테고리별 분기 (ex. 음료)
            List<PresentQueueDto.PresentDetail> presentQueDetailList = new ArrayList<>();
            for (PresentQueueDto.PresentDetail menu : presentQueueDto.getPresentDetailList()) {
                if (menu.getOrderAlarmTo() == OrderAlarmTo.SERVER) {
                    presentQueDetailList.add(menu);
                }
            }
            presentQueueDto.setPresentDetailList(presentQueDetailList); //주문정보 포함
            messagingTemplate.convertAndSend("/topic/ownerQue/" + key, presentQueueDto);

        } else {
            OrderQueueDto orderQueueDto = objectMapper.convertValue(eventQueDto.getPayload(), OrderQueueDto.class);
            List<OrderQueueDto.OrderingDetailInfo> orderQueList = new ArrayList<>();
            for (OrderQueueDto.OrderingDetailInfo menu : orderQueueDto.getOrderingDetailInfos()) {
                if (menu.getOrderAlarmTo() == OrderAlarmTo.SERVER) {
                    orderQueList.add(menu);
                }
            }
            orderQueueDto.setOrderingDetailInfos(orderQueList);
            messagingTemplate.convertAndSend("/topic/ownerQue/" + key, orderQueueDto);
        }
    }

    @KafkaListener(topics = "orderQue-topic", groupId = "kitchenQue-topic-group", containerFactory = "KitchenListener")
    public void kitchenQue(@Header(KafkaHeaders.RECEIVED_KEY) String key, String message) throws JsonProcessingException {
        EventQueDto eventQueDto = objectMapper.readValue(message, EventQueDto.class);
//      Que주문정보 분기(일반/선물)
        if ("PRESENT".equals(eventQueDto.getEventType())) {
            PresentQueueDto presentQueueDto = objectMapper.convertValue(eventQueDto.getPayload(), PresentQueueDto.class);
            //        카테고리별 분기 (ex. 음식)
            List<PresentQueueDto.PresentDetail> presentQueDetailList = new ArrayList<>();
            for (PresentQueueDto.PresentDetail menu : presentQueueDto.getPresentDetailList()) {
                if (menu.getOrderAlarmTo() == OrderAlarmTo.KITCHEN) {
                    presentQueDetailList.add(menu);
                }
            }
            presentQueueDto.setPresentDetailList(presentQueDetailList);
            messagingTemplate.convertAndSend("/topic/kitchenQue/" + key, presentQueueDto);

        } else {
            OrderQueueDto orderQueueDto = objectMapper.convertValue(eventQueDto.getPayload(), OrderQueueDto.class);
            List<OrderQueueDto.OrderingDetailInfo> orderQueList = new ArrayList<>();
            for (OrderQueueDto.OrderingDetailInfo menu : orderQueueDto.getOrderingDetailInfos()) {
                if (menu.getOrderAlarmTo() == OrderAlarmTo.KITCHEN) {
                    orderQueList.add(menu);
                }
            }
            orderQueueDto.setOrderingDetailInfos(orderQueList);
            messagingTemplate.convertAndSend("/topic/kitchenQue/" + key, orderQueueDto);
        }
    }
}

