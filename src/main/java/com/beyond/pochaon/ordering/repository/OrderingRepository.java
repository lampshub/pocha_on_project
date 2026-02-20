package com.beyond.pochaon.ordering.repository;

import com.beyond.pochaon.customerTable.domain.CustomerTable;
import com.beyond.pochaon.ordering.domain.OrderStatus;
import com.beyond.pochaon.ordering.domain.Ordering;
import com.beyond.pochaon.pay.domain.PaymentState;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderingRepository extends JpaRepository<Ordering, Long> {
    //    주문 조회
    List<Ordering> findByCustomerTableAndGroupId(
            CustomerTable customerTable,
            UUID groupId
    );

    List<Ordering> findByGroupId(UUID groupId);

    //    취소된 주문리스트 찾기
    List<Ordering> findByOrderStatusOrderByIdDesc(OrderStatus status);

    //    매장의 standBy 상태로 주문 조회
//    조인해서 매장별 필터링 / 오래된 주문이 위로
    @Query("SELECT DISTINCT o FROM Ordering o JOIN FETCH o.customerTable ct JOIN FETCH ct.store LEFT JOIN FETCH o.orderDetail od LEFT JOIN FETCH od.menu LEFT JOIN FETCH od.orderingDetailOptions WHERE ct.store.id = :storeId AND o.orderStatus = :status ORDER BY o.id ASC")
    List<Ordering> findByStoreIdAndOrderStatus(@Param("storeId") Long storeId, @Param("status") OrderStatus status);

    //groupId 목록으로 주문 + 상세 + 메뉴 + 옵션 한번에 조회하는 쿼리 (테이블 현황 조회시 사용)
    @Query("SELECT DISTINCT o FROM Ordering o LEFT JOIN FETCH o.orderDetail od LEFT JOIN FETCH od.menu LEFT JOIN FETCH od.orderingDetailOptions WHERE o.groupId IN :groupIds")
    List<Ordering> findAllWithDetailsByGroupIds(@Param("groupId") List<UUID> groupId);

    @Query("SELECT DISTINCT o FROM Ordering o LEFT JOIN FETCH o.orderDetail od LEFT JOIN FETCH od.menu LEFT JOIN FETCH od.orderingDetailOptions WHERE o.groupId IN :groupIds")
    List<Ordering> findAllWithDetailsByGroupId(@Param("groupId") UUID groupId);

    Ordering findByIdempotencyKey(UUID idempotencyKey);

    List<Ordering> findByGroupIdAndPaymentState(UUID groupId, PaymentState paymentState);

    //    정산용
//    특정 시간 내에 승인된 결제와 연관된 주문들을 조회
    // PaymentState가 COMPLETED인 것만 합산
    @Query("SELECT SUM(o.totalPrice) FROM Ordering o WHERE o.customerTable.store.id = :storeId AND o.paymentState = 'COMPLETED' AND o.createTimeAt >= :openedAt AND o.createTimeAt <= :closedAt")
    int sumTotalRevenue(@Param("storeId") Long storeId,
                         @Param("openedAt") LocalDateTime openedAt,
                         @Param("closedAt") LocalDateTime closedAt);

    @Query("SELECT COUNT(DISTINCT o.id) FROM Ordering o WHERE o.customerTable.store.id = :storeId AND o.paymentState = 'COMPLETED'AND o.createTimeAt >= :openedAt AND o.createTimeAt <= :closedAt")
    int countCompletedOrders(
            @Param("storeId") Long storeId,
            @Param("openedAt") LocalDateTime openedAt,
            @Param("closedAt") LocalDateTime closedAt);
}
