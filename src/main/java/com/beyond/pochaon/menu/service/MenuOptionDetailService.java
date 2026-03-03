package com.beyond.pochaon.menu.service;

import com.beyond.pochaon.common.auth.OwnerAuthHelper;
import com.beyond.pochaon.common.auth.OwnerAuthHelper.OwnerContext;
import com.beyond.pochaon.menu.domain.MenuOption;
import com.beyond.pochaon.menu.domain.MenuOptionDetail;
import com.beyond.pochaon.menu.dto.MenuOptionDetailReqDto;
import com.beyond.pochaon.menu.repository.MenuOptionDetailRepository;
import com.beyond.pochaon.menu.repository.MenuOptionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MenuOptionDetailService {

    private final MenuOptionRepository menuOptionRepository;
    private final MenuOptionDetailRepository menuOptionDetailRepository;
    private final OwnerAuthHelper ownerAuthHelper;

    @Autowired
    public MenuOptionDetailService(MenuOptionRepository menuOptionRepository, MenuOptionDetailRepository menuOptionDetailRepository, OwnerAuthHelper ownerAuthHelper) {
        this.menuOptionRepository = menuOptionRepository;
        this.menuOptionDetailRepository = menuOptionDetailRepository;
        this.ownerAuthHelper = ownerAuthHelper;
    }

    // 메뉴옵션상세 추가
    public Long createOptionDetail(Long optionId, MenuOptionDetailReqDto reqDto) throws AccessDeniedException {
        OwnerContext ctx = ownerAuthHelper.getOwnerContext();
        MenuOption menuOption = menuOptionRepository.findById(optionId)
                .orElseThrow(() -> new EntityNotFoundException("Option not found"));
        ownerAuthHelper.verifyAll(menuOption.getMenu().getCategory().getStore(), ctx);

        MenuOptionDetail menuOptionDetail = MenuOptionDetail.builder()
                .optionDetailName(reqDto.getOptionDetailName())
                .optionDetailPrice(reqDto.getOptionDetailPrice())
                .maxQuantity(reqDto.getMaxQuantity())
                .menuOption(menuOption)
                .build();

        menuOptionDetailRepository.save(menuOptionDetail);
//        menuOption.addMenuOptionDetail(menuOptionDetail);
        return menuOption.getId();
    }

    // 메뉴옵션상세 수정
    public void updateOptionDetail(Long detailId, MenuOptionDetailReqDto reqDto) throws AccessDeniedException {
        OwnerContext ctx = ownerAuthHelper.getOwnerContext();
        MenuOptionDetail menuOptionDetail = menuOptionDetailRepository.findById(detailId)
                .orElseThrow(() -> new EntityNotFoundException("OptionDetail not found"));
        ownerAuthHelper.verifyAll(menuOptionDetail.getMenuOption().getMenu().getCategory().getStore(), ctx);

        menuOptionDetail.update(reqDto.getOptionDetailName(), reqDto.getOptionDetailPrice(), reqDto.getMaxQuantity());
    }

    // 메뉴옵션상세 삭제
    public void deleteOptionDetail(Long detailId) throws AccessDeniedException {
        OwnerContext ctx = ownerAuthHelper.getOwnerContext();
        MenuOptionDetail menuOptionDetail = menuOptionDetailRepository.findById(detailId)
                .orElseThrow(() -> new EntityNotFoundException("OptionDetail not found"));
        ownerAuthHelper.verifyAll(menuOptionDetail.getMenuOption().getMenu().getCategory().getStore(), ctx);

        menuOptionDetailRepository.delete(menuOptionDetail);
    }
}
