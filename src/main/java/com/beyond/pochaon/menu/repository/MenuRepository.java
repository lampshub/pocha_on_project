package com.beyond.pochaon.menu.repository;


import com.beyond.pochaon.menu.domain.Menu;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {
    // 카테고리별 메뉴 조회
    List<Menu> findByCategoryId(Long categoryId);
    List<Menu> findByCategoryStoreId(Long storeId);

    // 메뉴 상세 조회 (옵션 + 옵션디테일 fetch join)
    @Query("""
                select distinct m
                from Menu m
                left join fetch m.menuOptionList mo
                where m.id = :menuId
            """)
    Optional<Menu> findDetailById(@Param("menuId") Long menuId);
}

