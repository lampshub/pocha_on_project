package com.beyond.pochaon.store.repository;

import com.beyond.pochaon.store.domain.Store;
import com.beyond.pochaon.store.domain.StoreSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StoreSettlementRepository extends JpaRepository<StoreSettlement, Long> {

    // 매장 + 날짜 단건 (unique constraint)
    Optional<StoreSettlement> findByStoreAndSettlementDate(Store store, LocalDate settlementDate);

    // 매장ID + 날짜 단건
    @Query("SELECT ss FROM StoreSettlement ss WHERE ss.store.id = :storeId AND ss.settlementDate = :date")
    Optional<StoreSettlement> findByStoreIdAndSettlementDate(
            @Param("storeId") Long storeId,
            @Param("date") LocalDate date);

    // 기간 범위 조회
    @Query("SELECT ss FROM StoreSettlement ss " +
            "WHERE ss.store.id = :storeId " +
            "AND ss.settlementDate >= :startDate AND ss.settlementDate < :endDate " +
            "ORDER BY ss.settlementDate")
    List<StoreSettlement> findByStoreIdAndSettlementDateRange(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // 최신 정산 1건
    @Query("SELECT ss FROM StoreSettlement ss WHERE ss.store.id = :storeId ORDER BY ss.settlementDate DESC LIMIT 1")
    Optional<StoreSettlement> findLatestByStoreId(@Param("storeId") Long storeId);

    // 오너의 모든 매장 정산 (월간)
    @Query("SELECT ss FROM StoreSettlement ss JOIN FETCH ss.store s " +
            "WHERE s.owner.id = :ownerId " +
            "AND ss.settlementDate >= :startDate AND ss.settlementDate < :endDate")
    List<StoreSettlement> findByOwnerIdAndMonth(
            @Param("ownerId") Long ownerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}