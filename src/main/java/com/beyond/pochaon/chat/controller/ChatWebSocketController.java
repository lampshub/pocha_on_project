package com.beyond.pochaon.chat.controller;


import com.beyond.pochaon.chat.domain.ChatMessage;
import com.beyond.pochaon.chat.dtos.ChatMessageDto;
import com.beyond.pochaon.chat.service.ChatService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat/send")
    public void sendMessage(@Payload ChatMessageDto messageDto) {

        ChatMessage savedMessage = chatService.sendMessage(messageDto);

        // 1. 채팅방 구독자에게 메시지 전송 (채팅창 표시용) - 기존 유지
        messagingTemplate.convertAndSend(
                "/topic/chat/" + messageDto.getChatRoomId(),
                savedMessage
        );

        // 2. 수신자에게 알림 전송 (badge/toast용) - 신규 추가
        // 수신자 테이블 번호로 발행하므로 프론트는 자기 번호만 구독하면 됨
        messagingTemplate.convertAndSend(
                "/topic/chat/notification/" + messageDto.getReceiverTableNum(),
                savedMessage  // ChatMessage에 이미 chatRoomId, senderTableNum 포함
        );
    }

    @MessageMapping("/chat/close")
    public void closeChatRoom(@Payload Map<String, Long> payload) {

        Long chatRoomId = payload.get("chatRoomId");

        chatService.closeChatRoom(chatRoomId);

        messagingTemplate.convertAndSend(
                "/topic/chat/" + chatRoomId + "/closed",
                Map.of(
                        "status", "closed",
                        "chatRoomId", chatRoomId
                )
        );
    }
}
