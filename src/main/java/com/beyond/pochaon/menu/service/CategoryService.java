package com.beyond.pochaon.menu.service;

import com.beyond.pochaon.common.auth.OwnerAuthHelper;
import com.beyond.pochaon.common.auth.OwnerAuthHelper.OwnerContext;
import com.beyond.pochaon.menu.domain.Category;
import com.beyond.pochaon.menu.dto.CategoryReqDto;
import com.beyond.pochaon.menu.repository.CategoryRepository;
import com.beyond.pochaon.store.domain.Store;
import com.beyond.pochaon.store.repository.StoreRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
public class CategoryService {
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;
    private final OwnerAuthHelper ownerAuthHelper;


    @Autowired
    public CategoryService(StoreRepository storeRepository, CategoryRepository categoryRepository, OwnerAuthHelper ownerAuthHelper) {
        this.storeRepository = storeRepository;
        this.categoryRepository = categoryRepository;
        this.ownerAuthHelper = ownerAuthHelper;
    }


    //    카테고리 추가
    public void createCategory(CategoryReqDto reqDto) {
        OwnerContext ctx = ownerAuthHelper.getOwnerContext();
        Store store = storeRepository.findById(ctx.getStoreId()).orElseThrow(() -> new EntityNotFoundException("Store not found"));
        ownerAuthHelper.verifyStoreOwnerShip(store, ctx.getOwner());
        Category category = reqDto.toEntity(store);
        categoryRepository.save(category);
    }


    //    카테고리 수정
    public void updateCategory(Long categoryId, CategoryReqDto reqDto) {
        OwnerContext ctx = ownerAuthHelper.getOwnerContext();
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));
        ownerAuthHelper.verifyAll(category.getStore(), ctx);
        category.updateName(reqDto.getCategoryName());
    }

    //    카테고리 삭제
    public void deleteCategory(Long categoryId) {
        OwnerContext ctx = ownerAuthHelper.getOwnerContext();
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));
        ownerAuthHelper.verifyAll(category.getStore(), ctx);
        categoryRepository.delete(category);
    }
}
