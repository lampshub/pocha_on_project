package com.beyond.pochaon.admin.dtos;

import com.beyond.pochaon.owner.domain.Owner;
import com.beyond.pochaon.store.domain.StoreStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OwnerListDto {

    private Long id;
    private String ownerName;
    private String ownerEmail;
    private String phoneNumber;
    private String businessRegistrationNumber;
    private LocalDateTime createTimeAt;
    private int approvedStoreCount;  //운영중인 매장수
    private int pendingStoreCount;  //승인대기중인 매장수

    public static OwnerListDto fromEntity(Owner owner){
        return OwnerListDto.builder()
                .id(owner.getId())
                .ownerName(owner.getOwnerName())
                .ownerEmail(owner.getOwnerEmail())
                .phoneNumber(owner.getPhoneNumber())
                .businessRegistrationNumber(owner.getBusinessRegistrationNumber())
                .createTimeAt(owner.getCreateTimeAt())
                .approvedStoreCount((int) owner.getStoreList().stream().filter(store -> store.getStatus() == StoreStatus.APPROVED).count())
                .pendingStoreCount((int) owner.getStoreList().stream().filter(store -> store.getStatus() == StoreStatus.PENDING).count())
                .build();
    }
}
