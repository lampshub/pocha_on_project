package com.beyond.pochaon.admin.domain;

import com.beyond.pochaon.owner.domain.Role;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 50)
    private String adminName;
    @Column(unique = true)
    private String adminEmail;
    private String password;
    @Column(unique = true)
    private String phoneNumber;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private Role role = Role.ADMIN;
}
