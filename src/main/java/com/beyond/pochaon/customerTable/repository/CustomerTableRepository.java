package com.beyond.pochaon.customerTable.repository;

import com.beyond.pochaon.customerTable.domain.CustomerTable;
import com.beyond.pochaon.customerTable.domain.TableStatus;
import com.beyond.pochaon.store.domain.Store;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerTableRepository extends JpaRepository<CustomerTable, Long> {
    List<CustomerTable> findByStore(Store store);
    Optional<CustomerTable> findByTableNum(Long tableNum);
    //    테이블 + 매장 fetch join(권한 검증용)
    @Query("SELECT ct FROM CustomerTable ct " +
            "JOIN FETCH ct.store " +
            "WHERE ct.customerTableId = :tableId")
    Optional<CustomerTable> findByIdWithStore(@Param("tableId") Long tableId);


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
}
