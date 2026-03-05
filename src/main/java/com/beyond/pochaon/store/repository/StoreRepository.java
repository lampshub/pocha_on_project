package com.beyond.pochaon.store.repository;

import com.beyond.pochaon.owner.domain.Owner;
import com.beyond.pochaon.store.domain.Store;
import com.beyond.pochaon.store.domain.StoreStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    List<Store> findByOwner(Owner owner);

    @Query("SELECT s FROM Store s WHERE s.owner.id = :ownerId")
    List<Store> findByOwnerId(@Param("ownerId") Long ownerId);

    // 상태값에 따른 정렬
    @Query("SELECT s FROM Store s WHERE (:status IS NULL OR s.status = :status) ORDER BY CASE WHEN s.status = 'PENDING' THEN 0 ELSE 1 END, s.serviceStartAt DESC NULLS LAST")
    Page<Store> findAllSortedByStatus(@Param("status") StoreStatus status, Pageable pageable);

//    서비스이용 자동갱신 && 서비스이용종료일 오늘
    List<Store> findByAutoRenewTrueAndServiceEndAt(LocalDate date);
}
