package com.beyond.pochaon.menu.repository;


import com.beyond.pochaon.menu.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuCategoryRepository extends JpaRepository<Category, Long> {

}