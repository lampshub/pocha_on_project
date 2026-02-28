package com.beyond.pochaon.chat.repository;

import com.beyond.pochaon.chat.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 특정 테이블이 참여 중인 활성 채팅방 목록 조회
    Optional<ChatRoom> findByRoomKeyAndIsActive(String roomKey, Boolean isActive);

    @Query("SELECT cr FROM ChatRoom cr " +
            "WHERE cr.storeId = :storeId " +
            "AND (cr.table1Num = :tableNum OR cr.table2Num = :tableNum) " +
            "AND cr.isActive = true")
        // 매장 전체 활성 채팅방 조회
    List<ChatRoom> findActiveRoomsByTable(
            @Param("storeId") Long storeId,
            @Param("tableNum") Integer tableNum
    );

    // 특정 테이블이 참여 중인 활성 채팅방 목록 조회
    List<ChatRoom> findByStoreIdAndIsActive(Long storeId, Boolean isActive);

    // 해당 채팅방이 존재하는지 boolean으로 빠르게 체크
    boolean existsByRoomKeyAndIsActive(String roomKey, Boolean isActive);

    @Query("SELECT COUNT(cr) FROM ChatRoom cr " +
            "WHERE cr.storeId = :storeId " +
            "AND (cr.table1Num = :tableNum OR cr.table2Num = :tableNum) " +
            "AND cr.isActive = true")
        // 특정 테이블이 현재 참여 중인 채팅 개수(채팅 제한 정책 구현용)
    long countActiveRoomsByTable(
            @Param("storeId") Long storeId,
            @Param("tableNum") Integer tableNum
    );

    Optional<ChatRoom> findByRoomKey(String roomKey);
}
