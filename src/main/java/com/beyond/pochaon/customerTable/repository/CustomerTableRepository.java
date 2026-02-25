package com.beyond.pochaon.customerTable.repository;

import com.beyond.pochaon.customerTable.domain.CustomerTable;
import com.beyond.pochaon.customerTable.domain.TableStatus;
import com.beyond.pochaon.store.domain.Store;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerTableRepository extends JpaRepository<CustomerTable, Long> {
    List<CustomerTable> findByStore(Store store);
    List<CustomerTable> findByStoreId(Long storeId);
    boolean existsByStoreIdAndTableNum(Long storeId, int tableNum);

    // 기존 메서드 교체: storeId 조건 추가 + 비관적 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ct FROM CustomerTable ct " +
            "WHERE ct.tableNum = :tableNum AND ct.store.id = :storeId")
    Optional<CustomerTable> findByTableNumAndStoreIdWithLock(
            @Param("tableNum") int tableNum,
            @Param("storeId") Long storeId
    );


    //    매장의 모든 테이블 + 매장 정보
    @Query("SELECT ct FROM CustomerTable ct " +
            "JOIN FETCH ct.store " +
            "WHERE ct.store.id = :storeId")
    List<CustomerTable> findByStoreIdWithStore(@Param("storeId") Long storeId);

    Optional<CustomerTable> findByStoreIdAndTableNum(Long storeId, Integer otherTableNum);

    List<CustomerTable> findByStoreIdAndTableStatusAndTableNumNot(
            Long storeId,
            TableStatus tableStatus,
            Integer tableNum
    );

    @Query("SELECT ct FROM CustomerTable ct " +
            "JOIN FETCH ct.store " +
            "WHERE ct.customerTableId = :customerTableId")
    Optional<CustomerTable> findByIdWithStore(@Param("customerTableId") Long customerTableId);

    @Query("SELECT ct FROM CustomerTable ct " +
            "WHERE ct.tableNum = :tableNum AND ct.store.id = :storeId")
    Optional<CustomerTable> findByTableNumAndStoreId(
            @Param("tableNum") int tableNum,
            @Param("storeId") Long storeId
    );
}
