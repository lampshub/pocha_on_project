package com.beyond.pochaon.admin.dtos;

import com.beyond.pochaon.store.domain.RenewalRequestStatus;
import com.beyond.pochaon.store.domain.Store;
import com.beyond.pochaon.store.domain.StoreRenewalRequest;
import com.beyond.pochaon.store.domain.StoreStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreListDtoOnAdmin {

    private Long id;
    private String StoreName;
    private String address;
    private StoreStatus status;
    private String ownerName;
    private String phoneNumber;
    private String businessRegistrationNumber;

    private LocalDateTime createTimeAt; //점주 서비스 신청일
    private LocalDate serviceStartAt;   //서비스이용 시작일(점주요청일)
    private LocalDateTime processedAt;      //승인/거절 처리일
    private String rejectedReason;
    @Builder.Default
    private boolean autoRenew = false;
    private Long renewId;
    private RenewalRequestStatus renewalRequestStatus;
    private LocalDateTime renewProcessedAt;
    private String renewRejectedReason;

    public static StoreListDtoOnAdmin fromEntity(Store store,  StoreRenewalRequest renewalRequest) {
        return StoreListDtoOnAdmin.builder()
                .id(store.getId())
                .StoreName(store.getStoreName())
                .address(store.getAddress())
                .status(store.getStatus())
                .ownerName(store.getOwner().getOwnerName())
                .phoneNumber(store.getPhoneNumber())
                .businessRegistrationNumber(store.getOwner().getBusinessRegistrationNumber())
                .createTimeAt(store.getCreateTimeAt())
                .serviceStartAt(store.getServiceStartAt())
                .processedAt(store.getProcessedAt())
                .rejectedReason(store.getRejectReason())
                .autoRenew(store.isAutoRenew())
                .renewId(renewalRequest.getId())
                .renewProcessedAt(renewalRequest.getProcessedAt())
                .renewRejectedReason(renewalRequest.getRejectedReason())
                .build();
    }
}