package com.beyond.pochaon.store.domain;

import com.beyond.pochaon.common.BaseTimeEntity;
import com.beyond.pochaon.owner.domain.Owner;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Store extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String storeName;
    private String address;
    private String phoneNumber;
    @Column(nullable = false)
    private LocalDate serviceStartAt;   //서비스 이용 시작일(점주요청일)
    private LocalDate serviceEndAt;     //서비스 이용 종료일(시작일로부터 1달)
    @Builder.Default
    private boolean autoRenew = false;          //서비스 자동연장 선택유무
    private LocalDateTime processedAt;  //사용 승인/거절 처리일
    private String rejectReason;        //사용 거절 사유

    private LocalTime storeOpenAt;
    private LocalTime storeCloseAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StoreStatus status = StoreStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    public void updateTime(LocalTime openAt, LocalTime closeAt) {
        this.storeOpenAt = openAt;
        this.storeCloseAt = closeAt;
    }

    public void approve() {
        this.status = StoreStatus.APPROVED;
        this.processedAt = LocalDateTime.now();
    }

    public void reject(String rejectedReason) {
        this.status = StoreStatus.REJECTED;
        this.processedAt = LocalDateTime.now();
        this.rejectReason = rejectedReason;
    }

    public void updateAutoRenew(boolean autoRenew){
        this.autoRenew = autoRenew;
    }

//    서비스기간 1달 연장
    public void extendOneMonth(){
        this.serviceEndAt = this.serviceEndAt.plusMonths(1);
    }

//    이용상태 계산 (승인상태가 APPROVED일때)
    public UsageStatus getUsageStatus(){
        LocalDate today = LocalDate.now();
        if(today.isBefore(serviceStartAt)){
            return UsageStatus.BEFORE_START;
        }
        if(today.isAfter(serviceEndAt)){
            return UsageStatus.EXPIRED;
        }
        return UsageStatus.ACTIVE;
    }

//    서비스이용기간 D-7 조회
    public boolean isExpiringSoon(){
        if(serviceEndAt == null) return false;
        LocalDate today = LocalDate.now();
        return !today.isAfter(serviceEndAt) && !today.isBefore(getServiceEndAt().minusDays(7));
    }


}
