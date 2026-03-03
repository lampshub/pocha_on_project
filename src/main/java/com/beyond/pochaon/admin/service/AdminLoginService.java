package com.beyond.pochaon.admin.service;

import com.beyond.pochaon.admin.domain.Admin;
import com.beyond.pochaon.admin.dtos.AdminLoginDto;
import com.beyond.pochaon.admin.repository.AdminRepository;
import com.beyond.pochaon.common.auth.JwtTokenProvider;
import com.beyond.pochaon.common.auth.TokenStage;
import com.beyond.pochaon.owner.dto.TokenDto;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminLoginService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    @Autowired
    public AdminLoginService(AdminRepository adminRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public TokenDto adminLogin(AdminLoginDto dto){
        Admin admin = adminRepository.findByAdminEmail(dto.getAdminEmail()).orElseThrow(()->new EntityNotFoundException("없는 이메일(계정)입니다"));
//        비밀번호 검증
        if(!passwordEncoder.matches(dto.getPassword(), admin.getPassword())){
            throw new EntityNotFoundException("비밀번호가 틀렸습니다");
        }
//        at, rt 생성
        String name = admin.getAdminName();
        String adminAccessToken = jwtTokenProvider.createAdminAccessToken(admin, TokenStage.BASE, null);
        String adminRefreshToken = jwtTokenProvider.createAdminRefreshToken(admin);
        return TokenDto.fromEntity(adminAccessToken, adminRefreshToken, name);
    }

    public TokenDto refresh(String refreshToken){

        // 1. RT 검증 + Admin 조회
        Admin admin = jwtTokenProvider.validateAdminRefreshToken(refreshToken);

        // 2. 새로운 토큰 발급
        String newAccessToken =
                jwtTokenProvider.createAdminAccessToken(
                        admin,
                        TokenStage.BASE,
                        null
                );

        String name = admin.getAdminName();
        String newRefreshToken =
                jwtTokenProvider.createAdminRefreshToken(admin);

        return TokenDto.fromEntity(newAccessToken, newRefreshToken, name);
    }
}
