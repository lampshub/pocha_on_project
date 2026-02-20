package com.beyond.pochaon.common.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseChatAlarmDto {

    private Long storeId;
    private int senderTable;      // 보낸 테이블     //채팅로직에 맞춰서 chatRoomId를 넣어야할지
    private int receiverTable;    // 받는 테이블
    private String message;     // 알림 메시지
}
