package com.beyond.pochaon.menu.service;

import com.beyond.pochaon.menu.domain.Menu;
import com.beyond.pochaon.menu.domain.MenuOption;
import com.beyond.pochaon.menu.domain.MenuOptionDetail;
import com.beyond.pochaon.menu.dtos.MenuOptionDetailReqDto;
import com.beyond.pochaon.menu.repository.MenuOptionDetailRepository;
import com.beyond.pochaon.menu.repository.MenuOptionRepository;
import com.beyond.pochaon.owner.domain.Owner;
import com.beyond.pochaon.owner.repository.OwnerRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MenuOptionDetailService {

    private final OwnerRepository ownerRepository;
    private final HttpServletRequest request;
    private final MenuOptionRepository menuOptionRepository;
    private final MenuOptionDetailRepository menuOptionDetailRepository;
    @Autowired
    public MenuOptionDetailService(OwnerRepository ownerRepository, HttpServletRequest request, MenuOptionRepository menuOptionRepository, MenuOptionDetailRepository menuOptionDetailRepository) {
        this.ownerRepository = ownerRepository;
        this.request = request;
        this.menuOptionRepository = menuOptionRepository;
        this.menuOptionDetailRepository = menuOptionDetailRepository;
    }

//    owner 메뉴옵션상세 추가
    public Long createOptionDetail(Long optionId, MenuOptionDetailReqDto reqDto) throws AccessDeniedException {
        String email = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Owner owner = ownerRepository.findByOwnerEmail(email).orElseThrow(() -> new EntityNotFoundException("Owner not found"));
        Long storeId = (Long) request.getAttribute("storeId");
        if (storeId == null) {
            throw new AccessDeniedException("매장 선택 후 이용가능합니다");
        }
        MenuOption menuOption = menuOptionRepository.findById(optionId).orElseThrow(()-> new EntityNotFoundException("option not found"));
        Menu menu = menuOption.getMenu();
        if (!menu.getStore().getId().equals(storeId)) {
            throw new AccessDeniedException("해당 매장의 메뉴가 아닙니다");
        }
        if (!menu.getStore().getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("해당 권한이 없습니다");
        }

        MenuOptionDetail menuOptionDetail = MenuOptionDetail.builder()
                .optionDetailName(reqDto.getOptionDetailName())
                .optionDetailPrice(reqDto.getOptionDetailPrice())
                .menuOption(menuOption)
                .build();

        menuOptionDetailRepository.save(menuOptionDetail);
//        menuOption.addMenuOptionDetail(menuOptionDetail);
        return menuOption.getId();
    }

//    owner 메뉴옵션상세 수정
    public void updateOptionDetail(Long detailId, MenuOptionDetailReqDto reqDto) throws AccessDeniedException {
        String email = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Owner owner = ownerRepository.findByOwnerEmail(email).orElseThrow(() -> new EntityNotFoundException("Owner not found"));
        Long storeId = (Long) request.getAttribute("storeId");
        if (storeId == null) {
            throw new AccessDeniedException("매장 선택 후 이용가능합니다");
        }
        MenuOptionDetail menuOptionDetail = menuOptionDetailRepository.findById(detailId).orElseThrow(()-> new EntityNotFoundException("OptionDetail not found"));
        MenuOption menuOption = menuOptionDetail.getMenuOption();
        Menu menu = menuOption.getMenu();
        if (!menu.getStore().getId().equals(storeId)) {
            throw new AccessDeniedException("해당 매장의 메뉴가 아닙니다");
        }
        if (!menu.getStore().getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("해당 권한이 없습니다");
        }
        menuOptionDetail.update(reqDto.getOptionDetailName(), reqDto.getOptionDetailPrice());
    }

//    owner 메뉴옵션상세 삭제
    public void deleteOptionDetail(Long detailId) throws AccessDeniedException {
        String email = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Owner owner = ownerRepository.findByOwnerEmail(email).orElseThrow(() -> new EntityNotFoundException("Owner not found"));
        Long storeId = (Long) request.getAttribute("storeId");
        if (storeId == null) {
            throw new AccessDeniedException("매장 선택 후 이용가능합니다");
        }
        MenuOptionDetail menuOptionDetail = menuOptionDetailRepository.findById(detailId).orElseThrow(()-> new EntityNotFoundException("OptionDetail not found"));
        MenuOption menuOption = menuOptionDetail.getMenuOption();
        Menu menu = menuOption.getMenu();
        if (!menu.getStore().getId().equals(storeId)) {
            throw new AccessDeniedException("해당 매장의 메뉴가 아닙니다");
        }
        if (!menu.getStore().getOwner().getId().equals(owner.getId())) {
        throw new AccessDeniedException("해당 권한이 없습니다");
    }
        menuOptionDetailRepository.delete(menuOptionDetail);
    }
}
