package com.beyond.pochaon.owner.service;

import com.beyond.pochaon.owner.dtos.BusinessApiReqDto;
import com.beyond.pochaon.owner.dtos.BusinessApiResDto;
import com.beyond.pochaon.owner.repository.OwnerVerifyClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OwnerService {
    private final OwnerVerifyClient ownerVerifyClient;

    @Autowired
    public OwnerService(OwnerVerifyClient ownerVerifyClient) {
        this.ownerVerifyClient = ownerVerifyClient;
    }

    public BusinessApiResDto verify(BusinessApiReqDto reqDto) {
        BusinessApiResDto resDto = ownerVerifyClient.verify(reqDto);

        if (resDto == null) {
            throw new IllegalStateException("사업자진위확인 API 응답이 없습니다.");
        }
        if (!resDto.getStatus_code().equals("OK")) {
            throw new IllegalStateException("사업자 확인 실패" + resDto.getMessage());
        }
        if (resDto.getData() == null || resDto.getData().isEmpty()) {
            throw new IllegalStateException("사업자 정보가 존재하지 않습니다.");
        }
        return resDto;
    }
}
