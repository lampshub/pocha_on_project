package com.beyond.pochaon.store.repository;

import com.beyond.pochaon.owner.domain.Owner;
import com.beyond.pochaon.store.domain.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    List<Store> findByOwner(Owner owner);

    @Query("SELECT s FROM Store s WHERE s.owner.id = :ownerId")
    List<Store> findByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT s FROM Store s JOIN FETCH s.owner ORDER BY CASE WHEN s.status = 'PENDING' THEN 0 ELSE 1 END ASC, CASE WHEN s.status = 'PENDING' THEN s.createTimeAt END ASC, s.processedAt DESC")
    Page<Store> findAllWithOwners(Pageable pageable);

//    서비스이용 자동갱신 && 서비스이용종료일 오늘
    List<Store> findByAutoRenewTrueAndServiceEndAt(LocalDate date);
}
