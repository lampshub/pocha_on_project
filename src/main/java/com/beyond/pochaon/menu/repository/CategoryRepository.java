package com.beyond.pochaon.menu.repository;


import com.beyond.pochaon.menu.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByIdAndStoreId(Long id, Long storeId);
    List<Category> findByStoreId(Long storeId);

    // [N+1 해결] 카테고리 + 메뉴 한번에 조회
    @Query("SELECT DISTINCT c FROM Category c " +
            "LEFT JOIN FETCH c.menuList " +
            "WHERE c.store.id = :storeId")
    List<Category> findByStoreIdWithMenus(@Param("storeId") Long storeId);
}