package com.beyond.pochaon.ingredient.repository;

import com.beyond.pochaon.ingredient.domain.Ingredient;
import com.beyond.pochaon.ingredient.domain.IngredientDetail;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface IngredientDetailRepository extends JpaRepository<IngredientDetail, Long> {
//    선입선출을 위해 사용 가능 재고 유통기한 순으로 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<IngredientDetail> findAllByIngredientAndCurrentQuantityGreaterThanOrderByDeadlineAsc(Ingredient ingredient, int quantity);

//    해당 식자재의 현재 재고의 총량
    @Query("SELECT SUM(d.currentQuantity) FROM IngredientDetail d WHERE d.ingredient = :ingredient")
    Integer sumCurrentQuantityByIngredient(@Param("ingredient") Ingredient ingredient);
  
    // 기간 내 재료 입고 총액 (정산용)
    @Query("SELECT COALESCE(SUM(d.totalPrice), 0) FROM IngredientDetail d " +
            "WHERE d.ingredient.store.id = :storeId " +
            "AND d.createTimeAt >= :startAt AND d.createTimeAt < :endAt")
    int sumTotalPriceByStoreAndPeriod(@Param("storeId") Long storeId,
                                      @Param("startAt") LocalDateTime startAt,
                                      @Param("endAt") LocalDateTime endAt);

    // 일별 재료 입고 비용 (캘린더용)
    @Query("SELECT CAST(d.createTimeAt AS date), COALESCE(SUM(d.totalPrice), 0) " +
            "FROM IngredientDetail d " +
            "WHERE d.ingredient.store.id = :storeId " +
            "AND d.createTimeAt >= :startAt AND d.createTimeAt < :endAt " +
            "GROUP BY CAST(d.createTimeAt AS date)")
    List<Object[]> sumDailyCostByStore(@Param("storeId") Long storeId,
                                       @Param("startAt") LocalDateTime startAt,
                                       @Param("endAt") LocalDateTime endAt);

    // ★ 유통기한 만료 + 잔량 있는 재료 (로스 대상)
    @Query("SELECT d FROM IngredientDetail d " +
            "JOIN FETCH d.ingredient i " +
            "WHERE i.store.id = :storeId " +
            "AND d.deadline IS NOT NULL " +
            "AND d.deadline < :now " +
            "AND d.currentQuantity > 0")
    List<IngredientDetail> findExpiredWithStock(@Param("storeId") Long storeId,
                                                @Param("now") LocalDateTime now);

    // ★ 유통기한 만료 + 잔량 0 (로스 기록 없이 삭제만)
    @Query("SELECT d FROM IngredientDetail d " +
            "JOIN FETCH d.ingredient i " +
            "WHERE i.store.id = :storeId " +
            "AND d.deadline IS NOT NULL " +
            "AND d.deadline < :now " +
            "AND d.currentQuantity = 0")
    List<IngredientDetail> findExpiredEmpty(@Param("storeId") Long storeId,
                                            @Param("now") LocalDateTime now);

    // ★ 유통기한 임박 재료 (N일 이내)
    @Query("SELECT d FROM IngredientDetail d " +
            "JOIN FETCH d.ingredient i " +
            "WHERE i.store.id = :storeId " +
            "AND d.deadline IS NOT NULL " +
            "AND d.deadline >= :now " +
            "AND d.deadline < :threshold " +
            "AND d.currentQuantity > 0 " +
            "ORDER BY d.deadline ASC")
    List<IngredientDetail> findExpiringSoon(@Param("storeId") Long storeId,
                                            @Param("now") LocalDateTime now,
                                            @Param("threshold") LocalDateTime threshold);

    // ★ 해당 매장 전체 입고 총액 (전체 기간)
    @Query("SELECT COALESCE(SUM(d.totalPrice), 0) FROM IngredientDetail d " +
            "WHERE d.ingredient.store.id = :storeId")
    int sumAllTotalPriceByStore(@Param("storeId") Long storeId);


    List<IngredientDetail> findAllByIngredientAndCurrentQuantityGreaterThanOrderByDeadlineDesc(Ingredient ingredient, int quantity);

    // ══════════════════════════════════════════════
    //  ★ 전체 매장 배치 쿼리 (N+1 방지)
    // ══════════════════════════════════════════════

    // 다중 매장 입고 총액 합산
    @Query("SELECT COALESCE(SUM(d.totalPrice), 0) FROM IngredientDetail d " +
            "WHERE d.ingredient.store.id IN :storeIds " +
            "AND d.createTimeAt >= :startAt AND d.createTimeAt < :endAt")
    int sumTotalPriceByStores(@Param("storeIds") List<Long> storeIds,
                              @Param("startAt") LocalDateTime startAt,
                              @Param("endAt") LocalDateTime endAt);

    // 다중 매장 일별 재료 비용 합산
    @Query("SELECT CAST(d.createTimeAt AS date), COALESCE(SUM(d.totalPrice), 0) " +
            "FROM IngredientDetail d " +
            "WHERE d.ingredient.store.id IN :storeIds " +
            "AND d.createTimeAt >= :startAt AND d.createTimeAt < :endAt " +
            "GROUP BY CAST(d.createTimeAt AS date)")
    List<Object[]> sumDailyCostByStores(@Param("storeIds") List<Long> storeIds,
                                        @Param("startAt") LocalDateTime startAt,
                                        @Param("endAt") LocalDateTime endAt);
}
