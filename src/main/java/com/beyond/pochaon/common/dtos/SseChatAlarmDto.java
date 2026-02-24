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
    private int senderTable;      // 보낸 테이블
//    private Long chatRoomId;      // ChatService에서 받음
//    @JsonProperty("receiverTableNum") // ChatService 필드명에 맞춤
    private int receiverTable;    // 받는 테이블
    private String message;     // 알림 메시지
}
