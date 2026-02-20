package com.beyond.pochaon.store.domain;

import com.beyond.pochaon.common.BaseTimeEntity;
import com.beyond.pochaon.owner.domain.Owner;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Store extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; //매장 아이디

    private String storeName;
    private String address;
    private LocalTime storeOpenAt;
    private LocalTime storeCloseAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    public void updateTime(LocalTime openAt, LocalTime closeAt) {
        this.storeOpenAt = openAt;
        this.storeCloseAt = closeAt;
    }
}
