package com.beyond.pochaon.common.service;
import com.beyond.pochaon.common.dtos.PasswordResetReqDto;
import com.beyond.pochaon.owner.domain.Owner;
import com.beyond.pochaon.owner.repository.OwnerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetService {
    private final OwnerRepository ownerRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailAuthService emailAuthService;

    public PasswordResetService(
            OwnerRepository ownerRepository, PasswordEncoder passwordEncoder, EmailAuthService emailAuthService) {
        this.ownerRepository = ownerRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailAuthService = emailAuthService;
    }

    public void resetPassword(PasswordResetReqDto dto){

        if(dto.getNewPassword().length() < 8)
            throw new RuntimeException("비밀번호 8자 이상 필요");


        // 1. 이메일 인증 여부 확인
        emailAuthService.checkVerified(dto.getEmail());

        // 2. 회원 조회
        Owner owner = ownerRepository.findByOwnerEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("회원 없음"));

        if(passwordEncoder.matches(dto.getNewPassword(), owner.getPassword()))
            throw new RuntimeException("기존 비밀번호와 동일");

        // 3. 비밀번호 암호화 후 변경
        String encoded = passwordEncoder.encode(dto.getNewPassword());
        owner.changePassword(encoded);

        ownerRepository.save(owner);

        // 4. 인증 상태 제거 (재사용 방지)
        emailAuthService.clear(dto.getEmail());
    }
}