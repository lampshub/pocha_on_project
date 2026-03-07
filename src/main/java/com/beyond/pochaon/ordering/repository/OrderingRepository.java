package com.beyond.pochaon.ordering.repository;

import com.beyond.pochaon.ordering.domain.OrderStatus;
import com.beyond.pochaon.ordering.domain.Ordering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderingRepository extends JpaRepository<Ordering, Long> {
    // 수정
    @Query("SELECT DISTINCT o FROM Ordering o " +
            "LEFT JOIN FETCH o.orderDetail od " +
            "LEFT JOIN FETCH od.menu " +
            "WHERE o.groupId = :groupId")
    List<Ordering> findByGroupId(@Param("groupId") UUID groupId);
    @Query("SELECT DISTINCT o FROM Ordering o " +
            "JOIN FETCH o.orderDetail od " +
            "JOIN FETCH od.menu m " +
            "JOIN FETCH m.category " +
            "WHERE o.groupId IN :groupIds")
    List<Ordering> findByGroupIdIn(@Param("groupIds") Collection<UUID> groupIds);

    //    매장의 standBy 상태로 주문 조회
//    조인해서 매장별 필터링 / 오래된 주문이 위로
    @Query("SELECT DISTINCT o FROM Ordering o JOIN FETCH o.customerTable ct JOIN FETCH ct.store LEFT JOIN " +
            "FETCH o.orderDetail od LEFT JOIN FETCH od.menu " +
            " WHERE ct.store.id = :storeId AND o.orderStatus = :status ORDER BY o.id ASC")
    List<Ordering> findByStoreIdAndOrderStatus(@Param("storeId") Long storeId, @Param("status") OrderStatus status);

    //groupId 목록으로 주문 + 상세 + 메뉴 + 옵션 한번에 조회하는 쿼리 (테이블 현황 조회시 사용)
    @Query("SELECT DISTINCT o FROM Ordering o LEFT JOIN FETCH o.orderDetail od LEFT JOIN FETCH od.menu WHERE o.groupId IN :groupId")
    List<Ordering> findAllWithDetailsByGroupIds(@Param("groupId") List<UUID> groupId);

    @Query("SELECT DISTINCT o FROM Ordering o " +
            "LEFT JOIN FETCH o.orderDetail od " +
            "LEFT JOIN FETCH od.menu " +
            "WHERE o.groupId = :groupId")
    List<Ordering> findAllWithDetailsByGroupId(@Param("groupId") UUID groupId);

    Ordering findByIdempotencyKey(UUID idempotencyKey);

    //    정산용
//    특정 시간 내에 승인된 결제와 연관된 주문들을 조회
    // PaymentStatus가 DONE 것만 합산
// OrderingRepository.java
    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Ordering o " +
            "WHERE o.customerTable.store.id = :storeId " +
            "AND o.paymentStatus = 'DONE' " +
            "AND o.createTimeAt >= :openedAt AND o.createTimeAt <= :closedAt")
    int sumTotalRevenue(@Param("storeId") Long storeId,
                        @Param("openedAt") LocalDateTime openedAt,
                        @Param("closedAt") LocalDateTime closedAt);

    @Query("SELECT COUNT(DISTINCT o.id) FROM Ordering o WHERE o.customerTable.store.id = :storeId AND o.paymentStatus = 'DONE'AND o.createTimeAt >= :openedAt AND o.createTimeAt <= :closedAt")
    int countCompletedOrders(
            @Param("storeId") Long storeId,
            @Param("openedAt") LocalDateTime openedAt,
            @Param("closedAt") LocalDateTime closedAt);

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Ordering o WHERE o.groupId = :groupId")
    int sumTotalPriceByGroupId(@Param("groupId") UUID groupId);


    // ══════════════════════════════════════════════
    //  신규 쿼리 - 매출 정산 분석용
    // ══════════════════════════════════════════════

    // 취소 주문 건수
    @Query("SELECT COUNT(DISTINCT o.id) FROM Ordering o " +
            "WHERE o.customerTable.store.id = :storeId " +
            "AND o.orderStatus = 'CANCELLED' " +
            "AND o.createTimeAt >= :startAt AND o.createTimeAt < :endAt")
    int countCancelledOrders(@Param("storeId") Long storeId,
                             @Param("startAt") LocalDateTime startAt,
                             @Param("endAt") LocalDateTime endAt);

    // 테이블별 이용 횟수 (distinct groupId 기준)
    @Query("SELECT COUNT(DISTINCT o.groupId) FROM Ordering o " +
            "WHERE o.customerTable.store.id = :storeId " +
            "AND o.paymentStatus = 'DONE' " +
            "AND o.createTimeAt >= :startAt AND o.createTimeAt < :endAt")
    int countDistinctGroupIds(@Param("storeId") Long storeId,
                              @Param("startAt") LocalDateTime startAt,
                              @Param("endAt") LocalDateTime endAt);

    // 기간 내 완료된 주문 전체 조회 (메뉴 분석, 결제 내역용)

    @Query("SELECT DISTINCT o FROM Ordering o " +
            "JOIN FETCH o.customerTable ct " +
            "LEFT JOIN FETCH o.orderDetail od " +
            "LEFT JOIN FETCH od.menu m " +
            "LEFT JOIN FETCH m.category " +
            "WHERE ct.store.id = :storeId " +
            "AND o.paymentStatus = 'DONE' " +
            "AND o.createTimeAt >= :startAt AND o.createTimeAt < :endAt " +
            "ORDER BY o.createTimeAt DESC")
    List<Ordering> findCompletedOrdersWithDetails(@Param("storeId") Long storeId,
                                                  @Param("startAt") LocalDateTime startAt,
                                                  @Param("endAt") LocalDateTime endAt);

    // 취소된 주문 조회 (일별 정산 모달용)
    @Query("SELECT DISTINCT o FROM Ordering o " +
            "JOIN FETCH o.customerTable ct " +
            "LEFT JOIN FETCH o.orderDetail od " +
            "LEFT JOIN FETCH od.menu m " +
            "LEFT JOIN FETCH m.category " +
            "WHERE ct.store.id = :storeId " +
            "AND o.orderStatus = 'CANCELLED' " +
            "AND o.createTimeAt >= :startAt AND o.createTimeAt < :endAt " +
            "ORDER BY o.createTimeAt DESC")
    List<Ordering> findCancelledOrdersWithDetails(@Param("storeId") Long storeId,
                                                  @Param("startAt") LocalDateTime startAt,
                                                  @Param("endAt") LocalDateTime endAt);

    // 시간대별 매출 집계
    // ORDER BY에 alias 대신 FUNCTION() 직접 사용
    @Query("SELECT FUNCTION('HOUR', o.createTimeAt), COALESCE(SUM(o.totalPrice), 0) " +
            "FROM Ordering o " +
            "WHERE o.customerTable.store.id = :storeId " +
            "AND o.paymentStatus = 'DONE' " +
            "AND o.createTimeAt >= :startAt AND o.createTimeAt < :endAt " +
            "GROUP BY FUNCTION('HOUR', o.createTimeAt) " +
            "ORDER BY FUNCTION('HOUR', o.createTimeAt)")
    List<Object[]> sumSalesByHour(@Param("storeId") Long storeId,
                                  @Param("startAt") LocalDateTime startAt,
                                  @Param("endAt") LocalDateTime endAt);

    // 요일별 매출 집계
    //  ORDER BY에 alias 대신 FUNCTION() 직접 사용
    @Query("SELECT FUNCTION('DAYOFWEEK', o.createTimeAt), COALESCE(SUM(o.totalPrice), 0) " +
            "FROM Ordering o " +
            "WHERE o.customerTable.store.id = :storeId " +
            "AND o.paymentStatus = 'DONE' " +
            "AND o.createTimeAt >= :startAt AND o.createTimeAt < :endAt " +
            "GROUP BY FUNCTION('DAYOFWEEK', o.createTimeAt) " +
            "ORDER BY FUNCTION('DAYOFWEEK', o.createTimeAt)")
    List<Object[]> sumSalesByDayOfWeek(@Param("storeId") Long storeId,
                                       @Param("startAt") LocalDateTime startAt,
                                       @Param("endAt") LocalDateTime endAt);

    // 테이블별 매출/건수 집계
    @Query("SELECT ct.tableNum, COALESCE(SUM(o.totalPrice), 0), COUNT(DISTINCT o.id) " +
            "FROM Ordering o JOIN o.customerTable ct " +
            "WHERE ct.store.id = :storeId " +
            "AND o.paymentStatus = 'DONE' " +
            "AND o.createTimeAt >= :startAt AND o.createTimeAt < :endAt " +
            "GROUP BY ct.tableNum " +
            "ORDER BY SUM(o.totalPrice) DESC")
    List<Object[]> sumSalesByTable(@Param("storeId") Long storeId,
                                   @Param("startAt") LocalDateTime startAt,
                                   @Param("endAt") LocalDateTime endAt);

    // 테이블별 회전율 (distinct groupId 수)
    @Query("SELECT ct.tableNum, COUNT(DISTINCT o.groupId) " +
            "FROM Ordering o JOIN o.customerTable ct " +
            "WHERE ct.store.id = :storeId " +
            "AND o.paymentStatus = 'DONE' " +
            "AND o.createTimeAt >= :startAt AND o.createTimeAt < :endAt " +
            "GROUP BY ct.tableNum " +
            "ORDER BY ct.tableNum")
    List<Object[]> countGroupsByTable(@Param("storeId") Long storeId,
                                      @Param("startAt") LocalDateTime startAt,
                                      @Param("endAt") LocalDateTime endAt);


    /**
     * 일별 매출/주문건수/취소건수 breakdown (주간·월간 달력용)
     * 반환: [날짜(java.sql.Date), 매출합계(Long), 완료주문수(Long), 취소주문수(Long)]
     */
    @Query(value = "SELECT DATE(o.create_time_at) AS sale_date, " +
            "COALESCE(SUM(CASE WHEN o.payment_Status = 'DONE' THEN o.total_price ELSE 0 END), 0), " +
            "COUNT(DISTINCT CASE WHEN o.payment_Status = 'DONE' THEN o.id END), " +
            "COUNT(DISTINCT CASE WHEN o.order_status = 'CANCELLED' THEN o.id END) " +
            "FROM ordering o " +
            "JOIN customer_table ct ON o.table_id = ct.customer_table_id " +
            "WHERE ct.store_id = :storeId " +
            "AND o.create_time_at >= :startAt AND o.create_time_at < :endAt " +
            "GROUP BY DATE(o.create_time_at) " +
            "ORDER BY sale_date",
            nativeQuery = true)
    List<Object[]> sumSalesByDate(@Param("storeId") Long storeId,
                                  @Param("startAt") LocalDateTime startAt,
                                  @Param("endAt") LocalDateTime endAt);



    // 다중 매장 합산 매출
    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Ordering o " +
            "WHERE o.customerTable.store.id IN :storeIds " +
            "AND o.paymentStatus = 'DONE' " +
            "AND o.createTimeAt >= :startAt AND o.createTimeAt < :endAt")
    int sumTotalRevenueByStores(@Param("storeIds") List<Long> storeIds,
                                @Param("startAt") LocalDateTime startAt,
                                @Param("endAt") LocalDateTime endAt);

    // 다중 매장 완료 주문 수
    @Query("SELECT COUNT(DISTINCT o.id) FROM Ordering o " +
            "WHERE o.customerTable.store.id IN :storeIds " +
            "AND o.paymentStatus = 'DONE' " +
            "AND o.createTimeAt >= :startAt AND o.createTimeAt < :endAt")
    int countCompletedOrdersByStores(@Param("storeIds") List<Long> storeIds,
                                     @Param("startAt") LocalDateTime startAt,
                                     @Param("endAt") LocalDateTime endAt);

    // 다중 매장 취소 주문 수
    @Query("SELECT COUNT(DISTINCT o.id) FROM Ordering o " +
            "WHERE o.customerTable.store.id IN :storeIds " +
            "AND o.orderStatus = 'CANCELLED' " +
            "AND o.createTimeAt >= :startAt AND o.createTimeAt < :endAt")
    int countCancelledOrdersByStores(@Param("storeIds") List<Long> storeIds,
                                     @Param("startAt") LocalDateTime startAt,
                                     @Param("endAt") LocalDateTime endAt);

    // 다중 매장 일별 매출 합산
    @Query(value = "SELECT DATE(o.create_time_at) AS sale_date, SUM(o.total_price) AS total " +
            "FROM ordering o " +
            "JOIN customer_table ct ON o.table_id = ct.customer_table_id " +
            "WHERE ct.store_id IN :storeIds " +
            "AND o.payment_Status= 'DONE' " +
            "AND o.create_time_at >= :startAt AND o.create_time_at < :endAt " +
            "GROUP BY DATE(o.create_time_at) ORDER BY sale_date",
            nativeQuery = true)
    List<Object[]> sumSalesByDateForStores(@Param("storeIds") List<Long> storeIds,
                                           @Param("startAt") LocalDateTime startAt,
                                           @Param("endAt") LocalDateTime endAt);

    // 매장별 매출/주문 수 (비교 탭용 — 1쿼리로 전 매장 조회)
    @Query(value = "SELECT ct.store_id, " +
            "COALESCE(SUM(o.total_price), 0), " +
            "COUNT(DISTINCT o.id) " +
            "FROM ordering o " +
            "JOIN customer_table ct ON o.table_id = ct.customer_table_id " +
            "WHERE ct.store_id IN :storeIds " +
            "AND o.payment_Status = 'DONE' " +
            "AND o.create_time_at >= :startAt AND o.create_time_at < :endAt " +
            "GROUP BY ct.store_id",
            nativeQuery = true)
    List<Object[]> sumRevenuePerStore(@Param("storeIds") List<Long> storeIds,
                                      @Param("startAt") LocalDateTime startAt,
                                      @Param("endAt") LocalDateTime endAt);

    // 다중 매장 시간대별 매출 합산
    @Query("SELECT FUNCTION('HOUR', o.createTimeAt), COALESCE(SUM(o.totalPrice), 0) " +
            "FROM Ordering o " +
            "WHERE o.customerTable.store.id IN :storeIds " +
            "AND o.paymentStatus = 'DONE' " +
            "AND o.createTimeAt >= :startAt AND o.createTimeAt < :endAt " +
            "GROUP BY FUNCTION('HOUR', o.createTimeAt) " +
            "ORDER BY FUNCTION('HOUR', o.createTimeAt)")
    List<Object[]> sumSalesByHourForStores(@Param("storeIds") List<Long> storeIds,
                                           @Param("startAt") LocalDateTime startAt,
                                           @Param("endAt") LocalDateTime endAt);

    // 다중 매장 요일별 매출 합산
    @Query("SELECT FUNCTION('DAYOFWEEK', o.createTimeAt), COALESCE(SUM(o.totalPrice), 0) " +
            "FROM Ordering o " +
            "WHERE o.customerTable.store.id IN :storeIds " +
            "AND o.paymentStatus = 'DONE' " +
            "AND o.createTimeAt >= :startAt AND o.createTimeAt < :endAt " +
            "GROUP BY FUNCTION('DAYOFWEEK', o.createTimeAt) " +
            "ORDER BY FUNCTION('DAYOFWEEK', o.createTimeAt)")
    List<Object[]> sumSalesByDayOfWeekForStores(@Param("storeIds") List<Long> storeIds,
                                                @Param("startAt") LocalDateTime startAt,
                                                @Param("endAt") LocalDateTime endAt);
}