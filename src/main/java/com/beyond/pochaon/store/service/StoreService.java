package com.beyond.pochaon.store.service;

import com.beyond.pochaon.common.auth.JwtTokenProvider;
import com.beyond.pochaon.owner.domain.Owner;
import com.beyond.pochaon.owner.domain.Role;
import com.beyond.pochaon.owner.repository.OwnerRepository;
import com.beyond.pochaon.store.domain.Store;
import com.beyond.pochaon.store.domain.StoreRenewalRequest;
import com.beyond.pochaon.store.domain.StoreStatus;
import com.beyond.pochaon.store.domain.UsageStatus;
import com.beyond.pochaon.store.dto.*;
import com.beyond.pochaon.store.repository.RenewalRequestRepository;
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
    private final RenewalRequestRepository renewalRequestRepository;
    @Autowired
    public StoreService(StoreRepository storeRepository, OwnerRepository ownerRepository, JwtTokenProvider jwtTokenProvider, RenewalRequestRepository renewalRequestRepository) {
        this.storeRepository = storeRepository;
        this.ownerRepository = ownerRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.renewalRequestRepository = renewalRequestRepository;
    }

    public void createStore(StoreCreateDto dto, String email) {
        Owner owner = ownerRepository.findByOwnerEmail(email).orElseThrow(() -> new EntityNotFoundException("없는 이메일입니다. store_ser_create부분"));
        Store store = Store.builder()
                .storeName(dto.getStoreName())
                .address(dto.getAddress())
                .phoneNumber(dto.getPhoneNumber())
                .owner(owner)
                .serviceStartAt(dto.getServiceStartAt())
                .serviceEndAt(dto.getServiceStartAt().plusMonths(1).minusDays(1))
                .autoRenew(dto.isAutoRenew())
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
        return storeList.stream().map(store -> {
            StoreRenewalRequest renewalRequest = renewalRequestRepository.findTopByStoreOrderByCreateTimeAtDesc(store).orElse(null);   //없으면 null
            return StoreListDto.fromEntity(store, renewalRequest);
        }).collect(Collectors.toList());
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
        if (store.getStatus() != StoreStatus.APPROVED){
            throw new IllegalStateException("승인되지 않은 매장입니다.");
        }
        UsageStatus usageStatus = store.getUsageStatus();
        if(usageStatus == UsageStatus.BEFORE_START){
            throw new IllegalStateException("서비스 이용 시작일 전 입니다.");
        }
        if(usageStatus == UsageStatus.EXPIRED){
            throw new IllegalStateException("서비스 이용기간 만료된 매장입니다.");
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

    @Transactional
    public void updateRenewal(Long storeId, String email){
        Owner owner = ownerRepository.findByOwnerEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Owner not found"));
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("Store not found"));
        if (!store.getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("해당 매장에 대한 권한이 없습니다");
        }
        if (store.isAutoRenew()) {
            throw new IllegalStateException("자동연장 중인 매장은 수동 연장 신청 불가");
        }

        StoreRenewalRequest renewalRequest = StoreRenewalRequest.builder()
                .store(store)
                .build();
        renewalRequestRepository.save(renewalRequest);
    }

    @Transactional
    public void updateAutoRenew(Long storeId, String email, Boolean autoRenew){
        Owner owner = ownerRepository.findByOwnerEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Owner not found"));
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("Store not found"));
        if (!store.getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("해당 매장에 대한 권한이 없습니다");
        }
        store.updateAutoRenew(autoRenew);

    }
}