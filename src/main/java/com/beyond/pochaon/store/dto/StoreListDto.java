package com.beyond.pochaon.store.dto;

import com.beyond.pochaon.store.domain.*;
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
public class StoreListDto {

    private Long id;
    private String StoreName;
    private String address;
    private String phoneNumber;
    private StoreStatus status;
    private UsageStatus usageStatus;
    private RenewalRequestStatus renewalRequestStatus; //연장 신청 상태

    private LocalDate serviceStartAt;
    private LocalDate serviceEndAt;
    @Builder.Default
    private boolean autoRenew = false;
    private boolean expiringSoon;   // 이용기간 D-7 만료 여부

    private LocalDateTime createTimeAt;     //서비스 신청일
    private LocalDateTime processedAt;      //추가 승인/거절 처리일
    private String rejectedReason;

    private LocalDateTime renewProcessedAt;      //연장 승인/거절 처리일
    private String renewRejectedReason;

    public static StoreListDto fromEntity(Store store, StoreRenewalRequest renewalRequest){
        return  StoreListDto.builder()
                .id(store.getId())
                .StoreName(store.getStoreName())
                .address(store.getAddress())
                .phoneNumber(store.getPhoneNumber())
                .status(store.getStatus())
                .usageStatus(store.getUsageStatus())
                .renewalRequestStatus(renewalRequest != null ? renewalRequest.getRequestStatus() : null)
                .serviceStartAt(store.getServiceStartAt())
                .serviceEndAt(store.getServiceEndAt())
                .autoRenew(store.isAutoRenew())
                .expiringSoon(store.isExpiringSoon())
                .createTimeAt(store.getCreateTimeAt())
                .processedAt(store.getProcessedAt())
                .rejectedReason(store.getRejectReason())
                .renewProcessedAt(renewalRequest != null ? renewalRequest.getProcessedAt() : null)
                .renewRejectedReason(renewalRequest != null ? renewalRequest.getRejectedReason() : null)
                .build();
    }
}
