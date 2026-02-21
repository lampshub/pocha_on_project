package com.beyond.pochaon.menu.repository;

import com.beyond.pochaon.menu.domain.MenuOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MenuOptionRepository extends JpaRepository<MenuOption, Long> {

}
