package com.beyond.pochaon.store.repository;

import com.beyond.pochaon.store.domain.StoreSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    @Query("SELECT ss FROM StoreSettlement ss JOIN FETCH ss.store WHERE ss.store.id = :storeId AND ss.createTimeAt >= :startDate AND ss.createTimeAt < :endDate ORDER BY ss.createTimeAt ASC")
    List<StoreSettlement> findByStoreIdAndMonth(@Param("storeId") Long storeId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT ss FROM StoreSettlement ss JOIN FETCH ss.store WHERE ss.store.id = :storeId " +
            "AND ss.createTimeAt >= :startDate AND ss.createTimeAt < :endDate " +
            "AND FUNCTION('DAY', ss.createTimeAt) = :day")
    Optional<StoreSettlement> findByStoreIdAndDay(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("day") int day);

}
