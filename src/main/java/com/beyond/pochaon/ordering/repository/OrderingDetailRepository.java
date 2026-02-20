package com.beyond.pochaon.ordering.repository;

import com.beyond.pochaon.ordering.domain.Ordering;
import com.beyond.pochaon.ordering.domain.OrderingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderingDetailRepository extends JpaRepository<OrderingDetail, Long> {
    List<OrderingDetail> findByOrdering(Ordering ordering);
}
