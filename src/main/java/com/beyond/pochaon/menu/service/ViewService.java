package com.beyond.pochaon.menu.service;

import com.beyond.pochaon.menu.dtos.CategoryViewDto;
import com.beyond.pochaon.menu.dtos.MenuDetailPageDto;
import com.beyond.pochaon.menu.dtos.MenuViewDto;
import com.beyond.pochaon.menu.domain.Category;
import com.beyond.pochaon.menu.domain.Menu;
import com.beyond.pochaon.menu.domain.MenuOption;
import com.beyond.pochaon.menu.domain.MenuOptionDetail;
import com.beyond.pochaon.menu.repository.MenuCategoryRepository;
import com.beyond.pochaon.menu.repository.MenuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


    @Service
    @Transactional(readOnly = true)
    public class ViewService {

        private final MenuRepository menuRepository;
        private final MenuCategoryRepository menuCategoryRepository;

        public ViewService(MenuRepository menuRepository, MenuCategoryRepository menuCategoryRepository) {
            this.menuRepository = menuRepository;
            this.menuCategoryRepository = menuCategoryRepository;
        }

        // 1 전체 메뉴 조회
        public List<MenuViewDto> findAllMenu() {

            List<Menu> menus = menuRepository.findAll();
            List<MenuViewDto> result = new ArrayList<>();

            for (Menu menu : menus) {

                MenuViewDto dto = MenuViewDto.builder()
                        .menuId(menu.getId())
                        .menuName(menu.getMenuName())
                        .menuPrice(menu.getPrice())
                        .imageUrl(menu.getMenuImageUrl())
                        .build();

                result.add(dto);
            }

            return result;
        }

        // 2 카테고리별 메뉴 조회
        public CategoryViewDto findByCategory(Long categoryId) {

            Category category = menuCategoryRepository.findById(categoryId).orElseThrow(() -> new IllegalArgumentException("카테고리가 없습니다"));

            List<Menu> menus = menuRepository.findByCategory_Id(categoryId);

            List<CategoryViewDto.mappingMenu> mappingMenuList = new ArrayList<>();

            for (Menu menu : menus) {
                CategoryViewDto.mappingMenu mapping =
                        CategoryViewDto.mappingMenu.builder()
                                .menuId(menu.getId())
                                .menuName(menu.getMenuName())
                                .menuPrice(menu.getPrice())
                                .imageUrl(menu.getMenuImageUrl())
                                .build();

                mappingMenuList.add(mapping);
            }

            return CategoryViewDto.builder()
                    .categoryId(category.getId())
                    .categoryName(category.getCategoryName())
                    .mappingMenuList(mappingMenuList)
                    .build();
        }

        // 3 메뉴 상세 조회
        public MenuDetailPageDto findMenuDetail(Long menuId) {

            Menu menu = menuRepository.findDetailById(menuId).orElseThrow(() -> new IllegalArgumentException("없는 메뉴입니다"));

            List<MenuDetailPageDto.mappingOption> mappingOptionList = new ArrayList<>();

            for (MenuOption option : menu.getMenuOptionList()) {

                List<MenuDetailPageDto.mappingOption.mappingOptionDetail>
                        mappingOptionDetailList = new ArrayList<>();

                for (MenuOptionDetail detail : option.getMenuOptionDetailList()) {

                    MenuDetailPageDto.mappingOption.mappingOptionDetail detailDto =
                            MenuDetailPageDto.mappingOption.mappingOptionDetail.builder()
                                    .optionDetailId(detail.getId())
                                    .optionDetailName(detail.getOptionDetailName())
                                    .optionDetailPrice(detail.getOptionDetailPrice())
                                    .build();

                    mappingOptionDetailList.add(detailDto);
                }

                MenuDetailPageDto.mappingOption optionDto =
                        MenuDetailPageDto.mappingOption.builder()
                                .optionId(option.getId())
                                .optionName(option.getOptionName())
                                .mappingOptionDetailList(mappingOptionDetailList)
                                .build();

                mappingOptionList.add(optionDto);
            }

            return MenuDetailPageDto.builder()
                    .menuId(menu.getId())
                    .menuName(menu.getMenuName())
                    .menuPrice(menu.getPrice())
                    .quantity(1) //기본값
                    .mappingOptionList(mappingOptionList)
                    .build();
        }
    }
