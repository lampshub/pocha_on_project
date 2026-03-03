package com.beyond.pochaon.ingredient.repository;

import com.beyond.pochaon.ingredient.domain.Ingredient;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Ingredient i where i.store.id = :storeId and i.name = :name")
    Optional<Ingredient> findByStoreIdAndNameWithLock(@Param("storeId") Long storeId, @Param("name") String name);


}
