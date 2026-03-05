package com.beyond.pochaon.admin.service;

import com.beyond.pochaon.admin.dtos.OwnerListDto;
import com.beyond.pochaon.admin.dtos.ReqServiceEndAtDto;
import com.beyond.pochaon.admin.dtos.StoreListDtoOnAdmin;
import com.beyond.pochaon.admin.dtos.StoreStatusDto;
import com.beyond.pochaon.owner.domain.Owner;
import com.beyond.pochaon.owner.repository.OwnerRepository;
import com.beyond.pochaon.store.domain.Store;
import com.beyond.pochaon.store.domain.StoreStatus;
import com.beyond.pochaon.store.repository.StoreRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageImpl;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminService {
    private final OwnerRepository ownerRepository;
    private final StoreRepository storeRepository;
    @Autowired
    public AdminService(OwnerRepository ownerRepository, StoreRepository storeRepository) {
        this.ownerRepository = ownerRepository;
        this.storeRepository = storeRepository;
    }

    @Transactional(readOnly = true)
    public List<OwnerListDto> findAllOwner(){
        List<Owner> ownerList = ownerRepository.findAllWithStores();
        return ownerList.stream().map(owner -> OwnerListDto.fromEntity(owner)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<StoreListDtoOnAdmin> findAllStore(Pageable pageable, StoreStatus status) {

        return storeRepository.findAllSortedByStatus(status, pageable)
                .map(store -> StoreListDtoOnAdmin.fromEntity(store));
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

    public void updateServiceEndAt(Long storeId, ReqServiceEndAtDto dto){
        Store store = storeRepository.findById(storeId).orElseThrow(()-> new EntityNotFoundException("Store not found"));
        if(!store.getServiceStartAt().isBefore(dto.getEndAt()) ){
            throw new IllegalArgumentException("서비스종료일을 오늘보다 이전으로 설정할수 없습니다");
        } else if (!LocalDate.now().isBefore(dto.getEndAt())) {
            throw new IllegalArgumentException("서비스종료일을 서비스시작일보다 이전으로 설정할수 없습니다");
        }
        store.updateServiceEndAt(dto.getEndAt());
    }
}