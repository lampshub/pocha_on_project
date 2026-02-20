package com.beyond.pochaon.menu.repository;


import com.beyond.pochaon.menu.domain.MenuOptionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface MenuOptionDetailRepository extends JpaRepository<MenuOptionDetail, Long> {
    @Query("select coalesce(sum(d.optionDetailPrice), 0) " +
            "from MenuOptionDetail d " +
            "where d.id in :detailIdList")
    int sumPriceByOptionDetailId(@Param("detailIdList") List<Long> detailIdList);


}
