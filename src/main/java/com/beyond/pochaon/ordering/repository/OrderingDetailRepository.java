package com.beyond.pochaon.ordering.repository;

import com.beyond.pochaon.ordering.domain.OrderingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderingDetailRepository extends JpaRepository<OrderingDetail, Long> {

    // 1단계(OrderingRepository)에서 Ordering + OrderingDetail + Menu를 가져온 뒤 호출
    // orderingDetailOptions(List) 1개만 fetch join
    // orderingDetailOptionDetails는 @BatchSize(size=100)로 IN 쿼리 1번 처리
    @Query("SELECT DISTINCT od FROM OrderingDetail od " +
            "LEFT JOIN FETCH od.orderingDetailOptions odo " +
            "WHERE od.ordering.id IN :orderingIds")
    List<OrderingDetail> fetchOptionsForOrderings(@Param("orderingIds") List<Long> orderingIds);
}