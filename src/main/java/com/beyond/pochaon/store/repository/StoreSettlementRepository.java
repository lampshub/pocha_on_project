package com.beyond.pochaon.store.repository;

import com.beyond.pochaon.store.domain.Store;
import com.beyond.pochaon.store.domain.StoreSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StoreSettlementRepository extends JpaRepository<StoreSettlement, Long> {

    StoreSettlement findByStoreId(Long storeId);

    // 특정 오너의 모든 매장 정산 - 해당 월
    @Query("SELECT ss FROM StoreSettlement ss JOIN FETCH ss.store s " +
            "WHERE s.owner.id = :ownerId " +
            "AND ss.createTimeAt >= :startDate AND ss.createTimeAt < :endDate")
    List<StoreSettlement> findByOwnerIdAndMonth(
            @Param("ownerId") Long ownerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // 변경
    List<StoreSettlement> findByStoreAndSettlementDate(Store store, LocalDate settlementDate);

    // 기존 findByStoreIdAndMonth 대체
    @Query("SELECT ss FROM StoreSettlement ss " +
            "WHERE ss.store.id = :storeId " +
            "AND ss.settlementDate >= :startDate AND ss.settlementDate < :endDate " +
            "ORDER BY ss.settlementDate")
    List<StoreSettlement> findByStoreIdAndSettlementDateRange(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // 기존 findByStoreIdAndDay 대체
    @Query("SELECT ss FROM StoreSettlement ss " +
            "WHERE ss.store.id = :storeId " +
            "AND ss.settlementDate = :date")
    Optional<StoreSettlement> findByStoreIdAndSettlementDate(
            @Param("storeId") Long storeId,
            @Param("date") LocalDate date);
}
