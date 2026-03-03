package com.beyond.pochaon.admin.service;

import com.beyond.pochaon.admin.dtos.OwnerListDto;
import com.beyond.pochaon.admin.dtos.RenewalStatusDto;
import com.beyond.pochaon.admin.dtos.StoreListDtoOnAdmin;
import com.beyond.pochaon.admin.dtos.StoreStatusDto;
import com.beyond.pochaon.owner.domain.Owner;
import com.beyond.pochaon.owner.repository.OwnerRepository;
import com.beyond.pochaon.store.domain.RenewalRequestStatus;
import com.beyond.pochaon.store.domain.Store;
import com.beyond.pochaon.store.domain.StoreRenewalRequest;
import com.beyond.pochaon.store.domain.StoreStatus;
import com.beyond.pochaon.store.repository.RenewalRequestRepository;
import com.beyond.pochaon.store.repository.StoreRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminService {
    private final OwnerRepository ownerRepository;
    private final StoreRepository storeRepository;
    private final RenewalRequestRepository renewalRequestRepository;
    @Autowired
    public AdminService(OwnerRepository ownerRepository, StoreRepository storeRepository, RenewalRequestRepository renewalRequestRepository) {
        this.ownerRepository = ownerRepository;
        this.storeRepository = storeRepository;
        this.renewalRequestRepository = renewalRequestRepository;
    }

    @Transactional(readOnly = true)
    public List<OwnerListDto> findAllOwner(){
        List<Owner> ownerList = ownerRepository.findAllWithStores();
        return ownerList.stream().map(owner -> OwnerListDto.fromEntity(owner)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<StoreListDtoOnAdmin> findAllStore(Pageable pageable) {
        Page<Store> storePage = storeRepository.findAllWithOwners(pageable);
        return storePage.map(store -> {
            StoreRenewalRequest renewalRequest = renewalRequestRepository.findTopByStoreOrderByCreateTimeAtDesc(store).orElse(null);
            return StoreListDtoOnAdmin.fromEntity(store, renewalRequest);
        });
    }
//    점주 매장신청 승인/거절
    public void updateStoreStatus(Long storeId, StoreStatusDto dto){
        Store store = storeRepository.findById(storeId).orElseThrow(()-> new EntityNotFoundException("Store not found"));
        if (store.getServiceStartAt() == null) {
            throw new IllegalStateException("서비스 시작일이 없습니다.");
        }
        if(dto.getStatus() == StoreStatus.APPROVED){
            store.approve();
        } else if(dto.getStatus() == StoreStatus.REJECTED){
            store.reject(dto.getRejectedReason());
        }
    }

//    점주 매장연장신청 승인/거절
    public void updateRenewalStatus(Long renewalId, RenewalStatusDto dto){
        StoreRenewalRequest requestStatus = renewalRequestRepository.findById(renewalId).orElseThrow(()-> new EntityNotFoundException("RequestStatus not found"));
        Store store = storeRepository.findById(requestStatus.getStore().getId()).orElseThrow(()->new EntityNotFoundException("store not found"));
        if(dto.getStatus() == RenewalRequestStatus.APPROVED){
            requestStatus.approve();
        }
        if(dto.getStatus() == RenewalRequestStatus.REJECTED){
            requestStatus.reject(dto.getRejectedReason());
        }
    }
}