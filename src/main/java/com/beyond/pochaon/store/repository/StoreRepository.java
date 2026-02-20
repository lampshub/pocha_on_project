package com.beyond.pochaon.store.repository;

import com.beyond.pochaon.owner.domain.Owner;
import com.beyond.pochaon.store.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    List<Store> findByOwner(Owner owner);

    // StoreRepository
    @Query("SELECT s FROM Store s WHERE s.owner.id = :ownerId")
    List<Store> findByOwnerId(@Param("ownerId") Long ownerId);
}
