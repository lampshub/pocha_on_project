package com.beyond.pochaon.ordering.repository;

import com.beyond.pochaon.ordering.domain.OrderingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderingDetailRepository extends JpaRepository<OrderingDetail, Long> {
}
