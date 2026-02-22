package com.beyond.pochaon.menu.service;

import com.beyond.pochaon.menu.domain.Category;
import com.beyond.pochaon.menu.dtos.CategoryReqDto;
import com.beyond.pochaon.menu.repository.CategoryRepository;
import com.beyond.pochaon.owner.domain.Owner;
import com.beyond.pochaon.owner.repository.OwnerRepository;
import com.beyond.pochaon.store.domain.Store;
import com.beyond.pochaon.store.repository.StoreRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;

@Service
@Transactional
public class CategoryService {
    private final OwnerRepository ownerRepository;
    private final HttpServletRequest request;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;
    @Autowired
    public CategoryService(OwnerRepository ownerRepository, HttpServletRequest request, StoreRepository storeRepository, CategoryRepository categoryRepository) {
        this.ownerRepository = ownerRepository;
        this.request = request;
        this.storeRepository = storeRepository;
        this.categoryRepository = categoryRepository;
    }


    //    카테고리 추가
    public Long createCategory(CategoryReqDto reqDto) throws AccessDeniedException {
        String email = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Owner owner = ownerRepository.findByOwnerEmail(email).orElseThrow(() -> new EntityNotFoundException("Owner not found"));
        Long storeId = (Long) request.getAttribute("storeId");
        if (storeId == null) {
            throw new AccessDeniedException("매장 선택 후 이용가능합니다");
        }
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new EntityNotFoundException("Store not found"));
        if (!store.getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("해당 권한이 없습니다");
        }
        Category category = reqDto.toEntity(store);
        categoryRepository.save(category);
        return category.getId();
    }


//    카테고리 수정
    public Long updateCategory(Long categoryId, CategoryReqDto reqDto) throws AccessDeniedException {
        String email = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Owner owner = ownerRepository.findByOwnerEmail(email).orElseThrow(() -> new EntityNotFoundException("Owner not found"));
        Long storeId = (Long) request.getAttribute("storeId");
        if (storeId == null) {
            throw new AccessDeniedException("매장 선택 후 이용가능합니다");
        }
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new EntityNotFoundException("Category not found"));
        if (!category.getStore().getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("해당 권한이 없습니다");
        }
        if (!category.getStore().getId().equals(storeId)) {
            throw new AccessDeniedException("해당 매장의 카테고리가 아닙니다");
        }

        category.updateName(reqDto.getCategoryName());
        return category.getId();
    }

//    카테고리 삭제
    public void deleteCategory(Long categoryId) throws AccessDeniedException {
        String email = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Owner owner = ownerRepository.findByOwnerEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Owner not found"));

        Long storeId = (Long) request.getAttribute("storeId");
        if (storeId == null) {
            throw new AccessDeniedException("해당 권한이 없습니다");
        }
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new EntityNotFoundException("Category not found"));
        if (!category.getStore().getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("해당 권한이 없습니다");
        }
        if (!category.getStore().getId().equals(storeId)) {
            throw new AccessDeniedException("해당 매장의 카테고리가 아닙니다");
        }

        categoryRepository.delete(category);

    }

}
