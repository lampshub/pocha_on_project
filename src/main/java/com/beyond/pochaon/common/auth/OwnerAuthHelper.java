package com.beyond.pochaon.common.auth;

import com.beyond.pochaon.owner.domain.Owner;
import com.beyond.pochaon.owner.repository.OwnerRepository;
import com.beyond.pochaon.store.domain.Store;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;


// 점주 인증 + 매장 권한 체크 공통 코드
@Component
public class OwnerAuthHelper {
    private final OwnerRepository ownerRepository;
    private final HttpServletRequest request;

    @Autowired
    public OwnerAuthHelper(OwnerRepository ownerRepository, HttpServletRequest request) {
        this.ownerRepository = ownerRepository;
        this.request = request;
    }

    //    현재 로그인한 owner 조회 + storeId 권한 확인
    public OwnerContext getOwnerContext() {
        String email = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Owner owner = ownerRepository.findByOwnerEmail(email).orElseThrow(() -> new EntityNotFoundException("Owner Not Found/ 사용변수 common_auth_OwnerAuthHelper_get OwnerContext"));

        Long storeId = (Long) request.getAttribute("storeId");
        if (storeId == null) {
            throw new AccessDeniedException("매장 선택 후 이용 가능/ 사용변수 common_auth_OwnerAuthHelper_get OwnerContext");
        }

        return new OwnerContext(owner, storeId);
    }

    //    Store의 소유자가 현재 로그인 한 Owner인지 검증
    public void verifyStoreOwnerShip(Store store, Owner owner) {
        if (!store.getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("해당 권한이 없습니다/ 사용변수 common_auth_OwnerAuthHelper_verifyStoreOwnerShip");
        }
    }

    //   Store가 요청된 StoreId와 일치하는지 검증
    public void verifyStoreMatch(Store store, Long storeId) {
        if (!store.getId().equals(storeId)) {
            throw new AccessDeniedException("해당 권한이 없습니다/ 사용변수 common_auth_OwnerAuthHelper_verifyStoreMatch");
        }
    }

    //    Store 소유권 + storeId 일치 한번에 검증
    public void verifyAll(Store store, OwnerContext context) {
        verifyStoreMatch(store, context.getStoreId());
        verifyStoreOwnerShip(store, context.getOwner());
    }

    @Getter
    public static class OwnerContext {
        private final Owner owner;
        private final Long storeId;

        public OwnerContext(Owner owner, Long storeId) {
            this.owner = owner;
            this.storeId = storeId;
        }
    }
}
