package com.beyond.pochaon.menu.service;

import com.beyond.pochaon.menu.domain.Category;
import com.beyond.pochaon.menu.domain.Menu;
import com.beyond.pochaon.menu.domain.MenuOption;
import com.beyond.pochaon.menu.domain.MenuOptionDetail;
import com.beyond.pochaon.menu.dtos.CategoryViewDto;
import com.beyond.pochaon.menu.dtos.MenuDetailPageDto;
import com.beyond.pochaon.menu.dtos.MenuViewDto;
import com.beyond.pochaon.menu.repository.CategoryRepository;
import com.beyond.pochaon.menu.repository.MenuRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;


    @Service
    @Transactional(readOnly = true)
    public class ViewService {

        private final MenuRepository menuRepository;
        private final CategoryRepository categoryRepository;
        private final HttpServletRequest request;

        @Autowired
        public ViewService(MenuRepository menuRepository, CategoryRepository categoryRepository, HttpServletRequest request) {
            this.menuRepository = menuRepository;
            this.categoryRepository = categoryRepository;
            this.request = request;
        }

        // 1 전체 메뉴 조회
        public List<MenuViewDto> findAllMenu() throws AccessDeniedException {

            Long storeId = (Long) request.getAttribute("storeId");
            if (storeId == null) {
                throw new AccessDeniedException("해당 권한이 없습니다");
            }

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
        public List<CategoryViewDto> findByCategory(Long categoryId) throws AccessDeniedException {

            Long storeId = (Long) request.getAttribute("storeId");
            if (storeId == null) {
                throw new AccessDeniedException("해당 권한이 없습니다");
            }

            Category category = categoryRepository.findByIdAndStoreId(categoryId, storeId).orElseThrow(() -> new IllegalArgumentException("카테고리가 없습니다"));

            List<Menu> menus = menuRepository.findByCategoryId(categoryId);

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

            CategoryViewDto dto = CategoryViewDto.builder()
                    .categoryId(category.getId())
                    .categoryName(category.getCategoryName())
                    .mappingMenuList(mappingMenuList)
                    .build();

            List<CategoryViewDto> result = new ArrayList<>();
            result.add(dto);

            return result;
        }

        // 3 메뉴 상세 조회
        public MenuDetailPageDto findMenuDetail(Long menuId) throws AccessDeniedException {

            Long storeId = (Long) request.getAttribute("storeId");
            if (storeId == null) {
                throw new AccessDeniedException("해당 권한이 없습니다");
            }

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


        //        4.
        public List<CategoryViewDto> findAllCategory() {
            List<Category> categoryList = categoryRepository.findAll();

            List<CategoryViewDto> result = new ArrayList<>();
            for (Category category : categoryList) {
                List<Menu> menuList = menuRepository.findByCategoryId(category.getId());
                List<CategoryViewDto.mappingMenu>mappingMenuList =new ArrayList<>();
                for(Menu menu: menuList){
                    mappingMenuList.add(CategoryViewDto.mappingMenu.builder()
                            .menuId(menu.getId())
                            .menuName(menu.getMenuName())
                            .menuPrice(menu.getPrice())
                            .imageUrl(menu.getMenuImageUrl())
                            .build());
                }

                result.add(CategoryViewDto.builder()
                        .categoryId(category.getId())
                        .categoryName(category.getCategoryName())
                        .mappingMenuList(mappingMenuList)
                        .build());
            }
            return result;

        }
    }
