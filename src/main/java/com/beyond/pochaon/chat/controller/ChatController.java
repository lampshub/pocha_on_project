package com.beyond.pochaon.chat.controller;

import com.beyond.pochaon.chat.domain.ChatMessage;
import com.beyond.pochaon.chat.domain.ChatRoom;
import com.beyond.pochaon.chat.dtos.ChatRoomCreateReqDto;
import com.beyond.pochaon.chat.dtos.ChatRoomDto;
import com.beyond.pochaon.chat.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // 채팅방 생성
    @PostMapping("/room")
    public ResponseEntity<ChatRoom> createChatRoom(
            @Valid @RequestBody ChatRoomCreateReqDto request,
            HttpServletRequest httpRequest
    ) {

        Long storeId = (Long) httpRequest.getAttribute("storeId");
        Integer tableNum = (Integer) httpRequest.getAttribute("tableNum");

        ChatRoom room = chatService.getOrCreateChatRoom(
                storeId,
                tableNum,
                request.getOtherTableNum()
        );

        return ResponseEntity.ok(room);
    }

    // 메시지 조회
    @GetMapping("/room/{chatRoomId}/messages")
    public ResponseEntity<List<ChatMessage>> getMessages(
            @PathVariable Long chatRoomId
    ) {
        return ResponseEntity.ok(
                chatService.getMessages(chatRoomId)
        );
    }

    // 읽음 처리
    @PostMapping("/room/{chatRoomId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long chatRoomId,
            @RequestParam Integer tableNum
    ) {
        chatService.markAsRead(chatRoomId, tableNum);
        return ResponseEntity.ok().build();
    }

    // 내 채팅방 목록
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomDto>> getMyActiveRooms(
            @RequestParam Long storeId,
            @RequestParam Integer tableNum
    ) {
        return ResponseEntity.ok(
                chatService.getMyActiveRooms(storeId, tableNum)
        );
    }

    // unread count
    @GetMapping("/room/{chatRoomId}/unread")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
            @PathVariable Long chatRoomId,
            @RequestParam Integer tableNum
    ) {

        int count = chatService.getUnreadCount(chatRoomId, tableNum);

        return ResponseEntity.ok(
                Map.of(
                        "chatRoomId", chatRoomId,
                        "tableNum", tableNum,
                        "unreadCount", count
                )
        );
    }

    // 전체 채팅방 종료
    @PostMapping("/table/{tableNum}/close-all")
    public ResponseEntity<Void> closeAllRooms(
            @PathVariable Integer tableNum,
            @RequestParam Long storeId
    ) {
        chatService.closeAllRoomsByTable(storeId, tableNum);
        return ResponseEntity.ok().build();
    }
}