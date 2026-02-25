package com.beyond.pochaon.payment.repository;

import com.beyond.pochaon.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByPaymentKey(String paymentKey);

    // ══════════════════════════════════════════════
    //  신규 쿼리 - 결제 분석용
    // ══════════════════════════════════════════════

    // 결제 수단별 합계 (method: "카드", "현금", "계좌이체" 등)
    @Query("SELECT p.method, COALESCE(SUM(p.amount), 0), COUNT(p.id) " +
            "FROM Payment p " +
            "WHERE p.tableNum IN (SELECT ct.tableNum FROM CustomerTable ct WHERE ct.store.id = :storeId) " +
            "AND p.status = 'DONE' " +
            "AND p.approveAt >= :startAt AND p.approveAt < :endAt " +
            "GROUP BY p.method")
    List<Object[]> sumByPaymentMethod(@Param("storeId") Long storeId,
                                      @Param("startAt") LocalDateTime startAt,
                                      @Param("endAt") LocalDateTime endAt);

    // 최근 결제 내역 (승인된 것만, 최신순)
    @Query("SELECT p FROM Payment p " +
            "WHERE p.tableNum IN (SELECT ct.tableNum FROM CustomerTable ct WHERE ct.store.id = :storeId) " +
            "AND p.status = 'DONE' " +
            "AND p.approveAt >= :startAt AND p.approveAt < :endAt " +
            "ORDER BY p.approveAt DESC")
    List<Payment> findRecentTransactions(@Param("storeId") Long storeId,
                                         @Param("startAt") LocalDateTime startAt,
                                         @Param("endAt") LocalDateTime endAt);

    // 특정 기간 카드 매출 합계
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.tableNum IN (SELECT ct.tableNum FROM CustomerTable ct WHERE ct.store.id = :storeId) " +
            "AND p.status = 'DONE' AND p.method = '카드' " +
            "AND p.approveAt >= :startAt AND p.approveAt < :endAt")
    int sumCardSales(@Param("storeId") Long storeId,
                     @Param("startAt") LocalDateTime startAt,
                     @Param("endAt") LocalDateTime endAt);

    // 특정 기간 현금 매출 합계
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.tableNum IN (SELECT ct.tableNum FROM CustomerTable ct WHERE ct.store.id = :storeId) " +
            "AND p.status = 'DONE' AND p.method = '현금' " +
            "AND p.approveAt >= :startAt AND p.approveAt < :endAt")
    int sumCashSales(@Param("storeId") Long storeId,
                     @Param("startAt") LocalDateTime startAt,
                     @Param("endAt") LocalDateTime endAt);

    // 특정 기간 계좌이체 매출 합계
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.tableNum IN (SELECT ct.tableNum FROM CustomerTable ct WHERE ct.store.id = :storeId) " +
            "AND p.status = 'DONE' AND p.method = '계좌이체' " +
            "AND p.approveAt >= :startAt AND p.approveAt < :endAt")
    int sumTransferSales(@Param("storeId") Long storeId,
                         @Param("startAt") LocalDateTime startAt,
                         @Param("endAt") LocalDateTime endAt);
}
