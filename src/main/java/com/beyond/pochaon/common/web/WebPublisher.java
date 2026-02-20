package com.beyond.pochaon.common.web;

import com.beyond.pochaon.customerorder.dto.OrderCreateDto;
import com.beyond.pochaon.present.dto.OwnerEventDto;
import com.beyond.pochaon.present.dto.PresentOwnerDto;
import com.beyond.pochaon.present.dto.PresentReceiverDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Component
public class WebPublisher {

    private final ChannelTopic orderTopic;
    private final ChannelTopic tableTopic;
    @Qualifier("pubsubRedisTemplate")
    private final RedisTemplate<String, OwnerEventDto> redisTemplate;
    @Qualifier("tableRedisTemplate")
    private final RedisTemplate<String,PresentReceiverDto> tableredisTemplate;

    public WebPublisher(@Qualifier("orderTopic") ChannelTopic orderTopic, @Qualifier("pubsubRedisTemplate") RedisTemplate<String, OwnerEventDto> redisTemplate, @Qualifier("tableTopic") ChannelTopic tableTopic,@Qualifier("tableRedisTemplate") RedisTemplate<String, PresentReceiverDto> tableredisTemplate) {
        this.orderTopic = orderTopic;
        this.redisTemplate = redisTemplate;
        this.tableTopic = tableTopic;

        this.tableredisTemplate = tableredisTemplate;
    }

    public void publish(OwnerEventDto eventDto) {
        redisTemplate.convertAndSend(orderTopic.getTopic(), eventDto); //t-order
    }

    public void tablePublish(PresentReceiverDto receiverDto) {
        tableredisTemplate.convertAndSend(tableTopic.getTopic(), receiverDto); //t-order
    }


}

