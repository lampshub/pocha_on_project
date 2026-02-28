package com.beyond.pochaon.chat.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_rooms",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_room_key",
                        columnNames = {"roomKey"}
                )
        },
        indexes = {
                @Index(name = "idx_store_active", columnList = "storeId,isActive"),
                @Index(name = "idx_table1_active", columnList = "storeId,table1Num,isActive"),
                @Index(name = "idx_table2_active", columnList = "storeId,table2Num,isActive")
        }
)
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long storeId;

    @Column(nullable = false)
    private Integer table1Num;

    @Column(nullable = false)
    private Integer table2Num;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime closedAt;

      // 채팅방 고유 키
      // 형식: {storeId}:{작은번호}:{큰번호}
    @Column(nullable = false, unique = true, length = 100)
    private String roomKey;

    @PrePersist
    public void generateRoomKey() {
        if (this.roomKey == null) {
            int minTable = Math.min(table1Num, table2Num);
            int maxTable = Math.max(table1Num, table2Num);
            this.roomKey = String.format("%d:%d:%d", storeId, minTable, maxTable);
        }
    }

    public void close() {
        this.isActive = false;
        this.closedAt = LocalDateTime.now();
    }

    public Integer getOtherTableNum(Integer myTableNum) {
        if (myTableNum.equals(table1Num)) {
            return table2Num;
        } else if (myTableNum.equals(table2Num)) {
            return table1Num;
        }
        throw new IllegalArgumentException("해당 테이블은 이 채팅방에 없습니다");
    }

    public void reopen() {
        this.isActive = true;
    }
}