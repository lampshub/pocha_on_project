package com.beyond.pochaon.ingredient.repository;

import com.beyond.pochaon.ingredient.domain.IngredientLoss;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface IngredientLossRepository extends JpaRepository<IngredientLoss, Long> {

    // 기간 내 로스 총액
    @Query("SELECT COALESCE(SUM(l.lossAmount), 0) FROM IngredientLoss l " +
            "WHERE l.store.id = :storeId " +
            "AND l.createTimeAt >= :startAt AND l.createTimeAt < :endAt")
    int sumLossAmountByPeriod(@Param("storeId") Long storeId,
                              @Param("startAt") LocalDateTime startAt,
                              @Param("endAt") LocalDateTime endAt);

    // 기간 내 로스 목록 (정산 탭용)
    @Query("SELECT l FROM IngredientLoss l " +
            "WHERE l.store.id = :storeId " +
            "AND l.createTimeAt >= :startAt AND l.createTimeAt < :endAt " +
            "ORDER BY l.createTimeAt DESC")
    List<IngredientLoss> findByStoreAndPeriod(@Param("storeId") Long storeId,
                                              @Param("startAt") LocalDateTime startAt,
                                              @Param("endAt") LocalDateTime endAt);

    // 전체 기간 로스 총액 (로스율 계산용)
    @Query("SELECT COALESCE(SUM(l.lossAmount), 0) FROM IngredientLoss l " +
            "WHERE l.store.id = :storeId")
    int sumAllLossAmountByStore(@Param("storeId") Long storeId);
}