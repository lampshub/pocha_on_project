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

        messagingTemplate.convertAndSend(
                "/topic/chat/" + messageDto.getChatRoomId(),
                savedMessage
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
