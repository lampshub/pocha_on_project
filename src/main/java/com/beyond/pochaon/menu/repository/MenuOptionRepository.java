package com.beyond.pochaon.menu.repository;

import com.beyond.pochaon.menu.domain.MenuOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuOptionRepository extends JpaRepository<MenuOption, Long> {

        // 2단계: MenuOption + MenuOptionDetail
        @Query("SELECT DISTINCT mo FROM MenuOption mo " +
                "LEFT JOIN FETCH mo.menuOptionDetailList " +
                "WHERE mo.menu.id = :menuId")
        List<MenuOption> fetchOptionDetailsForMenu(@Param("menuId") Long menuId);
    
}
