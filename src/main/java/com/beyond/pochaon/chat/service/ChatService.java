package com.beyond.pochaon.chat.service;

import com.beyond.pochaon.chat.domain.ChatMessage;
import com.beyond.pochaon.chat.domain.ChatRoom;
import com.beyond.pochaon.chat.dtos.ChatMessageDto;
import com.beyond.pochaon.chat.dtos.ChatRoomDto;
import com.beyond.pochaon.chat.repository.ChatRoomRepository;
import com.beyond.pochaon.customerTable.domain.CustomerTable;
import com.beyond.pochaon.customerTable.repository.CustomerTableRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final CustomerTableRepository customerTableRepository;
    @Qualifier("chatRedis")
    private final RedisTemplate<String, Object> chatRedisTemplate;
    @Qualifier("ssePubSubChat")
    private final RedisTemplate<String, String> ssePubSubChatRedisTemplate;

    private final ObjectMapper objectMapper;
    private static final String MESSAGE_LIST_KEY = "chat:room:%d:messages";
    private static final String UNREAD_COUNT_KEY = "chat:room:%d:unread:%d";
    private static final String LAST_MESSAGE_KEY = "chat:room:%d:last";
    private static final Duration TTL = Duration.ofHours(24);

    public ChatService(ChatRoomRepository chatRoomRepository, CustomerTableRepository customerTableRepository, @Qualifier("chatRedis") RedisTemplate<String, Object> chatRedisTemplate, @Qualifier("ssePubSubChat")
    RedisTemplate<String, String > ssePubSubChatRedisTemplate, ObjectMapper objectMapper) {
        this.chatRoomRepository = chatRoomRepository;
        this.customerTableRepository = customerTableRepository;
        this.chatRedisTemplate = chatRedisTemplate;
        this.ssePubSubChatRedisTemplate = ssePubSubChatRedisTemplate;
        this.objectMapper = objectMapper;
    }

    // ===============================
    // 채팅방 생성 or 조회
    // ===============================
    @Transactional
    public ChatRoom getOrCreateChatRoom(
            Long storeId,
            Integer myTableNum,
            Integer otherTableNum
    ) {
        CustomerTable otherTable = customerTableRepository
                .findByStoreIdAndTableNum(storeId, otherTableNum)
                .orElseThrow(() -> new IllegalArgumentException(
                        otherTableNum + "번 테이블을 찾을 수 없습니다"));

        if (!"USING".equals(otherTable.getTableStatus().name())) {
            throw new IllegalStateException(
                    otherTableNum + "번 테이블은 현재 사용 중이 아닙니다.");
        }

        int min = Math.min(myTableNum, otherTableNum);
        int max = Math.max(myTableNum, otherTableNum);

        String roomKey = storeId + ":" + min + ":" + max;

        return chatRoomRepository
                .findByRoomKeyAndIsActive(roomKey, true)
                .orElseGet(() -> {
                    ChatRoom room = ChatRoom.builder()
                            .storeId(storeId)
                            .table1Num(min)
                            .table2Num(max)
                            .isActive(true)
                            .build();

                    ChatRoom saved = chatRoomRepository.save(room);

                    log.info("채팅방 생성 id={} {}↔{}", saved.getId(), min, max);
                    return saved;
                });
    }

    // ===============================
    // 메시지 전송
    // ===============================
    public ChatMessage sendMessage(ChatMessageDto dto) {

        ChatMessage message = ChatMessage.create(
                dto.getChatRoomId(),
                dto.getSenderTableNum(),
                dto.getMessage()
        );

        String listKey = MESSAGE_LIST_KEY.formatted(dto.getChatRoomId());

        chatRedisTemplate.opsForList().rightPush(listKey, message);

        // 최근 100개만 유지
        chatRedisTemplate.opsForList().trim(listKey, -100, -1);

        chatRedisTemplate.expire(listKey, TTL);

        // 마지막 메시지 저장
        chatRedisTemplate.opsForValue().set(
                LAST_MESSAGE_KEY.formatted(dto.getChatRoomId()),
                dto.getMessage(),
                TTL
        );

        incrementUnreadCount(dto.getChatRoomId(), dto.getReceiverTableNum());
//        알람레디스로 연결
        Map<String, Object> alarmData = Map.of(
                "storeId", dto.getStoreId(),
                "receiverTableNum", dto.getReceiverTableNum(),
                "chatRoomId", dto.getChatRoomId(),
                "message", dto.getMessage()
        );

        try {
            ssePubSubChatRedisTemplate.convertAndSend("chatting-channel",objectMapper.writeValueAsString(alarmData));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return message;
    }

    // ===============================
    // 메시지 조회
    // ===============================
    public List<ChatMessage> getMessages(Long chatRoomId) {

        String key = MESSAGE_LIST_KEY.formatted(chatRoomId);

        List<Object> cached = chatRedisTemplate.opsForList().range(key, 0, -1);

        if (cached == null || cached.isEmpty())
            return Collections.emptyList();

        return cached.stream()
                .map(obj -> (ChatMessage) obj)
                .collect(Collectors.toList());
    }

    // ===============================
    // 읽지않음 증가
    // ===============================
    private void incrementUnreadCount(Long roomId, Integer tableNum) {

        String key = UNREAD_COUNT_KEY.formatted(roomId, tableNum);

        chatRedisTemplate.opsForValue().increment(key);
        chatRedisTemplate.expire(key, TTL);
    }

    // ===============================
    // 읽지않음 조회
    // ===============================
    public int getUnreadCount(Long roomId, Integer tableNum) {

        Object value = chatRedisTemplate.opsForValue()
                .get(UNREAD_COUNT_KEY.formatted(roomId, tableNum));

        return value == null ? 0 : ((Number)value).intValue();
    }

    // ===============================
    // 읽음 처리
    // ===============================
    public void markAsRead(Long roomId, Integer tableNum) {

        chatRedisTemplate.delete(
                UNREAD_COUNT_KEY.formatted(roomId, tableNum)
        );
    }

    // ===============================
    // 채팅방 종료
    // ===============================
    @Transactional
    public void closeChatRoom(Long roomId) {

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 없음"));

        room.close();
        chatRoomRepository.save(room);

        chatRedisTemplate.delete(MESSAGE_LIST_KEY.formatted(roomId));
        chatRedisTemplate.delete(LAST_MESSAGE_KEY.formatted(roomId));

        // unread 전체 삭제
        Set<String> keys =
                chatRedisTemplate.keys("chat:room:" + roomId + ":unread:*");

        if (keys != null && !keys.isEmpty())
            chatRedisTemplate.delete(keys);

        log.info("채팅방 종료 id={}", roomId);
    }

    // ===============================
    // 내 활성 채팅방 목록
    // ===============================
    @Transactional(readOnly = true)
    public List<ChatRoomDto> getMyActiveRooms(Long storeId, Integer tableNum) {

        List<ChatRoom> rooms =
                chatRoomRepository.findActiveRoomsByTable(storeId, tableNum);

        return rooms.stream()
                .map(room -> {

                    Integer other = room.getOtherTableNum(tableNum);

                    String last = (String) chatRedisTemplate.opsForValue()
                            .get(LAST_MESSAGE_KEY.formatted(room.getId()));

                    return ChatRoomDto.builder()
                            .id(room.getId())
                            .storeId(room.getStoreId())
                            .table1Num(room.getTable1Num())
                            .table2Num(room.getTable2Num())
                            .otherTableNum(other)
                            .isActive(room.getIsActive())
                            .unreadCount(getUnreadCount(room.getId(), tableNum))
                            .lastMessage(last)
                            .createdAt(room.getCreatedAt())
                            .build();
                })
                .toList();
    }

    // ===============================
    // 해당 테이블 채팅방 전체 종료
    // ===============================
    @Transactional
    public void closeAllRoomsByTable(Long storeId, Integer tableNum) {

        List<ChatRoom> rooms =
                chatRoomRepository.findActiveRoomsByTable(storeId, tableNum);

        rooms.forEach(room -> closeChatRoom(room.getId()));

        log.info("{}번 테이블 채팅방 {}개 종료", tableNum, rooms.size());
    }
}
