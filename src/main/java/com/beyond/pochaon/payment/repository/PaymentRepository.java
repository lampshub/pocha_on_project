package com.beyond.pochaon.payment.repository;

import com.beyond.pochaon.payment.entity.Payment;
import com.beyond.pochaon.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByPaymentKey(String paymentKey);

    List<Payment> findByTableNumAndStatus(Integer tableNum, PaymentStatus status);
}
