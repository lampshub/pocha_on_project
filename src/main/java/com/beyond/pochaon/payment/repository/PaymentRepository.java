package com.beyond.pochaon.payment.repository;

import com.beyond.pochaon.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByPaymentKey(String paymentKey);

    List<Payment> findByGroupIdIn(Collection<String> groupIds);

    // 결제 수단별 합계 (method: "카드", "간편결제", "계좌이체" 등)
    @Query("SELECT p.method, COALESCE(SUM(p.amount), 0), COUNT(p.id) " +
            "FROM Payment p " +
            "WHERE p.storeId = :storeId " +
            "AND p.status = 'DONE' " +
            "AND p.approveAt >= :startAt AND p.approveAt < :endAt " +
            "GROUP BY p.method")
    List<Object[]> sumByPaymentMethod(@Param("storeId") Long storeId,
                                      @Param("startAt") LocalDateTime startAt,
                                      @Param("endAt") LocalDateTime endAt);

    // 최근 결제 내역 (승인된 것만, 최신순)
    @Query("SELECT p FROM Payment p " +
            "WHERE p.storeId = :storeId " +
            "AND p.status = 'DONE' " +
            "AND p.approveAt >= :startAt AND p.approveAt < :endAt " +
            "ORDER BY p.approveAt DESC")
    List<Payment> findRecentTransactions(@Param("storeId") Long storeId,
                                         @Param("startAt") LocalDateTime startAt,
                                         @Param("endAt") LocalDateTime endAt);
}