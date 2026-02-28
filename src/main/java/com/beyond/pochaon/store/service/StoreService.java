package com.beyond.pochaon.store.service;

import com.beyond.pochaon.common.auth.JwtTokenProvider;
import com.beyond.pochaon.owner.domain.Owner;
import com.beyond.pochaon.owner.domain.Role;
import com.beyond.pochaon.owner.repository.OwnerRepository;
import com.beyond.pochaon.store.domain.Store;
import com.beyond.pochaon.store.dtos.*;
import com.beyond.pochaon.store.repository.StoreRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class StoreService {

    private final StoreRepository storeRepository;
    private final OwnerRepository ownerRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public StoreService(StoreRepository storeRepository, OwnerRepository ownerRepository, JwtTokenProvider jwtTokenProvider) {
        this.storeRepository = storeRepository;
        this.ownerRepository = ownerRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public void createStore(StoreCreateDto dto, String email) {
        Owner owner = ownerRepository.findByOwnerEmail(email).orElseThrow(() -> new EntityNotFoundException("없는 이메일입니다. store_ser_create부분"));
        Store store = Store.builder()
                .storeName(dto.getName())
                .address(dto.getAddress())
                .storeOpenAt(dto.getOpenAt())
                .storeCloseAt(dto.getCloseAt())
                .owner(owner)
                .build();
        storeRepository.save(store);
    }

    @Transactional(readOnly = true)
    public List<StoreListDto> findAll(String email) {
        Owner owner = ownerRepository.findByOwnerEmail(email).orElseThrow(() -> new EntityNotFoundException("없는 entity. store_ser_findAll"));
        List<Store> storeList;
        if (owner.getRole() == Role.ADMIN) {
            storeList = storeRepository.findAll();
        } else {
            storeList = storeRepository.findByOwner(owner);
        }
        return storeList.stream().map(StoreListDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StoreTokenDto selectStore(String email, Long storeId) {
        Owner owner = ownerRepository.findByOwnerEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Owner not found"));
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("Store not found"));
        if (!store.getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("해당 매장에 대한 권한이 없습니다.");
        }
        String storeAccessToken = jwtTokenProvider.createStoreAccessToken(owner, storeId);
        return new StoreTokenDto(storeAccessToken);
    }

    @Transactional(readOnly = true)
    public StoreTimeResDto getStoreHours(Long storeId) {
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new IllegalStateException("Store not found"));
        return new StoreTimeResDto(store.getStoreOpenAt(), store.getStoreCloseAt());
    }

    @Transactional
    public void updateTime(Long storeId, StoreUpdateTimeDto dto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Owner owner = ownerRepository.findByOwnerEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Owner not found"));
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("Store not found"));
        if (!store.getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("해당 매장에 대한 권한이 없습니다");
        }
        if (dto.getOpenAt().equals(dto.getCloseAt())) {
            throw new IllegalStateException("오픈시간과 마감시간은 같을 수 없습니다");
        }
        store.updateTime(dto.getOpenAt(), dto.getCloseAt());
    }
}