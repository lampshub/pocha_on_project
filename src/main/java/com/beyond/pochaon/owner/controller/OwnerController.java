package com.beyond.pochaon.owner.controller;

import com.beyond.pochaon.common.auth.JwtTokenProvider;
import com.beyond.pochaon.owner.dtos.*;
import com.beyond.pochaon.owner.service.OwnerLoginService;
import com.beyond.pochaon.owner.service.OwnerMyPageService;
import com.beyond.pochaon.owner.service.OwnerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/owner")
public class OwnerController {
    private final OwnerService ownerService;
    private final OwnerLoginService ownerLoginService;
    private final OwnerMyPageService ownerMyPageService;

    @Autowired
    public OwnerController(OwnerService ownerService, OwnerLoginService ownerLoginService, JwtTokenProvider jwtTokenProvider, OwnerMyPageService ownerMyPageService) {
        this.ownerService = ownerService;
        this.ownerLoginService = ownerLoginService;
        this.ownerMyPageService = ownerMyPageService;
    }

    //    사업자등록번호 진위확인 OwnerController -> OwnerService -> OwnerVerifyClient
    @PostMapping("/business/verify")
    public BusinessApiResDto validateBusinessNumber(@Valid @RequestBody BusinessApiReqDto reqDto) {
        return ownerService.verify(reqDto);
    }

    @PostMapping("/baseLogin")
    public TokenDto baseLogin(@RequestBody BaseLoginDto dto) {
        return ownerLoginService.baseLogin(dto);
    }

    //    at만료시 redirect, rt기반으로 at재발급
    @PostMapping("/refresh")
    public TokenDto refresh(@RequestHeader("Authorization") String bearer) {
        String refreshToken = bearer.substring(7);
        return ownerLoginService.refresh(refreshToken);
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public void ownerCreate(@RequestBody OwnerCreateDto dto) {
        ownerLoginService.ownerSave(dto);
    }

    //    mypage 조회
    @GetMapping("/mypage")
    public ResponseEntity<MyPageResDto> getMyPage() {
        MyPageResDto response = ownerMyPageService.getMyPage();
        return ResponseEntity.ok(response);
    }

    //    mypage에서 비밀번호 변경
    @PutMapping("/mypage/updatepassword")
    public ResponseEntity<?> updatePassword(@RequestBody UpdatePasswordReqDto reqDto) {
        ownerMyPageService.myPageUpdatePassword(reqDto);
        return ResponseEntity.ok("비밀번호 변경 완료");
    }

    @PostMapping("/verify-password")
    public ResponseEntity<?> verifyPassword(
            Authentication authentication, // UserDetails 대신 Authentication 주입
            @RequestBody OwnerPasswordVerifyDto dto) {

        // 1. 인증 객체 체크
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증 정보가 없습니다.");
        }

        // 2. 이메일(Username) 추출
        // SecurityContext에 이메일이 저장되도록 필터가 설정되어 있다면 getName()으로 가져옵니다.
        String email = authentication.getName();

        boolean isMatched = ownerLoginService.verifyAndReleaseTable(email, dto.getPassword(), dto.getCustomerTableId());

        if (isMatched) {
            return ResponseEntity.ok("인증에 성공하였습니다.");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("비밀번호가 일치하지 않습니다.");
        }
    }
}
