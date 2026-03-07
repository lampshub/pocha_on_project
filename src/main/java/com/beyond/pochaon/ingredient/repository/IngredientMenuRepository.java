package com.beyond.pochaon.ingredient.repository;

import com.beyond.pochaon.ingredient.domain.Ingredient;
import com.beyond.pochaon.ingredient.domain.IngredientMenu;
import com.beyond.pochaon.menu.domain.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IngredientMenuRepository extends JpaRepository<IngredientMenu, Long> {
    void deleteByMenu(Menu menu);

//    메뉴의 식자재 레시피 조회
    List<IngredientMenu> findByMenuId(Long menuId);

    /**
     * 매장의 모든 메뉴별 원가를 계산한다.
     * 각 식자재의 가장 최근 입고 단가(unitPrice)를 기준으로
     * usageAmount * unitPrice 를 합산하여 메뉴 1개당 원가를 반환한다.
     * 반환: [menuId(Long), costPerUnit(Double)]
     */
    @Query(value = "SELECT im.menu_id, " +
            "SUM(im.usage_amount * COALESCE((" +
            "  SELECT id2.unit_price FROM ingredient_detail id2 " +
            "  WHERE id2.ingredient_id = im.ingredient_id " +
            "  AND id2.ingredient_detail_id = (" +
            "    SELECT MAX(id3.ingredient_detail_id) FROM ingredient_detail id3 " +
            "    WHERE id3.ingredient_id = im.ingredient_id" +
            "  )" +
            "), 0)) AS cost_per_unit " +
            "FROM ingredient_menu im " +
            "JOIN ingredient i ON im.ingredient_id = i.ingredient_id " +
            "WHERE i.store_id = :storeId " +
            "GROUP BY im.menu_id",
            nativeQuery = true)
    List<Object[]> findMenuCostByStoreId(@Param("storeId") Long storeId);

    boolean existsByIngredient(Ingredient ingredient);

    // IngredientMenuRepository.java에 추가
    @Query("SELECT im FROM IngredientMenu im " +
            "JOIN FETCH im.ingredient " +
            "WHERE im.menu.id = :menuId")
    List<IngredientMenu> findByMenuIdWithIngredient(@Param("menuId") Long menuId);
}
