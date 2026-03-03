package com.beyond.pochaon.store.repository;

import com.beyond.pochaon.store.domain.Store;
import com.beyond.pochaon.store.domain.StoreRenewalRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RenewalRequestRepository extends JpaRepository<StoreRenewalRequest, Long> {

     Optional<StoreRenewalRequest> findTopByStoreOrderByCreateTimeAtDesc(Store store);
}
