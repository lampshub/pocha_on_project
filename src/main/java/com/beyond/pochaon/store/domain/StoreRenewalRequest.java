package com.beyond.pochaon.store.domain;

import com.beyond.pochaon.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Getter
@Builder
public class StoreRenewalRequest  extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;
    private String rejectedReason;
    private LocalDateTime processedAt;  //연장 승인/거절 처리일
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RenewalRequestStatus requestStatus = RenewalRequestStatus.PENDING;

    public void approve(){
        this.requestStatus = RenewalRequestStatus.APPROVED;
    }
    public void reject(String rejectedReason){
        this.requestStatus = RenewalRequestStatus.REJECTED;
        this.rejectedReason = rejectedReason;
    }

}
