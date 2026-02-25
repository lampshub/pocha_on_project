package com.beyond.pochaon.ordering.repository;

import com.beyond.pochaon.ordering.domain.OrderingDetailOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderingDetailOptionRepository extends JpaRepository<OrderingDetailOption, Long> {
}
