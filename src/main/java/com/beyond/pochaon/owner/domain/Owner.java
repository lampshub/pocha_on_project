package com.beyond.pochaon.owner.domain;

import com.beyond.pochaon.common.BaseTimeEntity;
import com.beyond.pochaon.store.domain.Store;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Owner extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 10, unique = true)
    @Pattern(regexp = "^\\d{10}$", message = "사업자등록번호는 10자리 숫자여야 합니다")
    private String businessRegistrationNumber; //사업자 등록 번호
    private String password;
    @Column(unique = true)
    private String phoneNumber;
    @Column(length = 50)
    private String ownerName;
    @Column(unique = true)
    private String ownerEmail;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private Role role = Role.OWNER;
    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
    private List<Store> storeList;


    public void changePassword(String password){
        this.password = password;
    }

    public void updatePhoneNum(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
