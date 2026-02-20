package com.beyond.pochaon.chat.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    @NotNull(message = "채팅방 ID는 필수입니다")
    private Long chatRoomId;

    @NotNull(message = "매장 ID는 필수입니다")
    private Long storeId;

    @NotNull(message = "발신자 테이블 번호는 필수입니다")
    private Integer senderTableNum;

    @NotNull(message = "수신자 테이블 번호는 필수입니다")
    private Integer receiverTableNum;

    @NotBlank(message = "메시지는 필수입니다")
    private String message;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime createdAt;
}
