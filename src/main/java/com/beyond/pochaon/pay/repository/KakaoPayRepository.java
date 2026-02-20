package com.beyond.pochaon.pay.repository;

import com.beyond.pochaon.pay.domain.KakaoPay;
import com.beyond.pochaon.pay.domain.KakaoPayStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 카카오페이 결제 정보 Repository
 */
@Repository
public interface KakaoPayRepository extends JpaRepository<KakaoPay, Long> {
    
    Optional<KakaoPay> findByTid(String tid);
    
    Optional<KakaoPay> findByPartnerOrderId(String partnerOrderId);
    
    List<KakaoPay> findByStatus(KakaoPayStatus status);
    
    List<KakaoPay> findByPartnerUserId(String partnerUserId);
    
    Optional<KakaoPay> findByPartnerOrderIdAndStatus(String partnerOrderId, KakaoPayStatus status);

}