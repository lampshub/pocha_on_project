package com.beyond.pochaon.chat.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    // 채팅방 ID
    private Long chatRoomId;

    // 발신자 테이블 번호
    private Integer senderTableNum;

    // 메시지 내용
    private String message;

    // 생성 시간
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime createdAt;

    // 새 메시지 생성
    public static ChatMessage create(
            Long chatRoomId,
            Integer senderTableNum,
            String message
    ) {
        return ChatMessage.builder()
                .chatRoomId(chatRoomId)
                .senderTableNum(senderTableNum)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
