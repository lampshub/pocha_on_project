package com.beyond.pochaon.admin.controller;

import com.beyond.pochaon.admin.dtos.*;
import com.beyond.pochaon.admin.service.AdminLoginService;
import com.beyond.pochaon.admin.service.AdminService;
import com.beyond.pochaon.owner.dto.TokenDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final AdminLoginService adminLoginService;
    private final AdminService adminService;

    @Autowired
    public AdminController(AdminLoginService adminLoginService, AdminService adminService) {
        this.adminLoginService = adminLoginService;
        this.adminService = adminService;
    }

    @PostMapping("/login")
    public TokenDto adminLogin(@RequestBody AdminLoginDto dto) {
        return adminLoginService.adminLogin(dto);
    }

    //    at만료시 redirect, rt기반으로 at재발급
    @PostMapping("/refresh")
    public TokenDto refresh(@RequestHeader("Authorization") String bearer) {
        String refreshToken = bearer.substring(7);
        return adminLoginService.refresh(refreshToken);
    }

    @GetMapping("/ownerlist")
    public ResponseEntity<?> findAllOwner() {

        List<OwnerListDto> dtoList = adminService.findAllOwner();
        return ResponseEntity.status(HttpStatus.OK).body(dtoList);
    }

    @GetMapping("/storelist")
    public ResponseEntity<Page<StoreListDtoOnAdmin>> findAllStore(@PageableDefault(size = 10) Pageable pageable){
        Page<StoreListDtoOnAdmin> dtoPage = adminService.findAllStore(pageable);
        return ResponseEntity.status(HttpStatus.OK).body(dtoPage);
    }

    @PatchMapping("/{storeId}/status")
    public ResponseEntity<?> updateStoreStatus(@PathVariable Long storeId, @RequestBody StoreStatusDto dto){
         adminService.updateStoreStatus(storeId, dto);
         return ResponseEntity.status(HttpStatus.OK).body("매장상태가 " + dto.getStatus() + "로 변경되었습니다");
    }

    @PatchMapping("/renewal/{renewalId}/status")
    public ResponseEntity<?> updateRenewalStatus(@PathVariable Long renewalId, @RequestBody RenewalStatusDto dto){
        adminService.updateRenewalStatus(renewalId, dto);
        return ResponseEntity.status(HttpStatus.OK).body("매장 연장요청 상태가 "+ dto.getStatus()+"로 변경되었습니다");
    }
}
