package com.beyond.pochaon.owner.service;

import com.beyond.pochaon.common.service.EmailAuthService;
import com.beyond.pochaon.owner.domain.Owner;
import com.beyond.pochaon.owner.dtos.MyPageResDto;
import com.beyond.pochaon.owner.dtos.UpdatePasswordReqDto;
import com.beyond.pochaon.owner.repository.OwnerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OwnerMyPageService {
    private final OwnerRepository ownerRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailAuthService emailAuthService;
    @Autowired
    public OwnerMyPageService(OwnerRepository ownerRepository, PasswordEncoder passwordEncoder, EmailAuthService emailAuthService) {
        this.ownerRepository = ownerRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailAuthService = emailAuthService;
    }

//    mypage 조회
    public MyPageResDto getMyPage() {
        String email = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Owner owner = ownerRepository.findByOwnerEmail(email).orElseThrow(() -> new EntityNotFoundException("Owner not found"));
        return MyPageResDto.fromEntity(owner);
    }


    //    mypage에서 비밀번호 변경
    public void myPageUpdatePassword(UpdatePasswordReqDto reqDto){
        if(reqDto.getNewPassword().length() < 8)
            throw new RuntimeException("8자리 이상 입력해주세요.");

        String email = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Owner owner = ownerRepository.findByOwnerEmail(email).orElseThrow(() -> new RuntimeException("Owner not found"));

//        기존 비밀번호와 입력된 비밀번호 검증
        if (!passwordEncoder.matches(reqDto.getOldPassword(), owner.getPassword())) {
            throw new IllegalArgumentException("기존 비밀번호가 일치하지 않습니다");
        }

//        기존 비밀번호와 새 비밀번호가 같은지 검증
        if(passwordEncoder.matches(reqDto.getOldPassword(), reqDto.getNewPassword())) {
            throw new RuntimeException("기존 비밀번호와 동일한 비밀번호를 입력했습니다");
        }
        owner.changePassword(passwordEncoder.encode(reqDto.getNewPassword()));

//        ownerRepository.save(owner);
////        rt 삭제
//        redisTemplate.delete(owner.getOwnerEmail());

    }
}
