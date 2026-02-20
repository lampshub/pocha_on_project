package com.beyond.pochaon.customerorder.repository;


import com.beyond.pochaon.ordering.domain.Ordering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Ordering,Long> {
}
