package com.beyond.pochaon.menu.service;

import com.beyond.pochaon.common.auth.OwnerAuthHelper;
import com.beyond.pochaon.common.auth.OwnerAuthHelper.OwnerContext;
import com.beyond.pochaon.menu.domain.Menu;
import com.beyond.pochaon.menu.domain.MenuOption;
import com.beyond.pochaon.menu.domain.SelectionType;
import com.beyond.pochaon.menu.dto.MenuOptionReqDto;
import com.beyond.pochaon.menu.repository.MenuOptionRepository;
import com.beyond.pochaon.menu.repository.MenuRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.access.AccessDeniedException;

@Service
@Transactional
public class MenuOptionService {
    private final MenuOptionRepository menuOptionRepository;
    private final MenuRepository menuRepository;
    private final OwnerAuthHelper ownerAuthHelper;

    @Autowired
    public MenuOptionService(MenuOptionRepository menuOptionRepository, MenuRepository menuRepository, OwnerAuthHelper ownerAuthHelper) {
        this.menuOptionRepository = menuOptionRepository;
        this.menuRepository = menuRepository;
        this.ownerAuthHelper = ownerAuthHelper;
    }

    // 메뉴옵션 추가
    public Long createOption(Long menuId, MenuOptionReqDto reqDto) throws AccessDeniedException {
        OwnerContext ctx = ownerAuthHelper.getOwnerContext();
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new EntityNotFoundException("Menu not found"));
        ownerAuthHelper.verifyAll(menu.getCategory().getStore(), ctx);

        MenuOption menuOption = MenuOption.builder()
                .optionName(reqDto.getOptionName())
                .minSelect(reqDto.getMinSelect())
                .maxSelect(reqDto.getMaxSelect())
                .selectionType(reqDto.getSelectionType())
                .menu(menu)
                .build();
        menuOptionRepository.save(menuOption);
        return menuOption.getId();
    }

    // 메뉴옵션 수정
    public void updateOption(Long optionId, MenuOptionReqDto reqDto) throws AccessDeniedException {
        OwnerContext ctx = ownerAuthHelper.getOwnerContext();
        MenuOption menuOption = menuOptionRepository.findById(optionId)
                .orElseThrow(() -> new EntityNotFoundException("Option not found"));
        ownerAuthHelper.verifyAll(menuOption.getMenu().getCategory().getStore(), ctx);

        menuOption.update(reqDto.getOptionName(),reqDto.getSelectionType(), reqDto.getMaxSelect(), reqDto.getMinSelect());
    }




    // 메뉴옵션 삭제
    public void deleteOption(Long optionId) throws AccessDeniedException {
        OwnerContext ctx = ownerAuthHelper.getOwnerContext();
        MenuOption menuOption = menuOptionRepository.findById(optionId)
                .orElseThrow(() -> new EntityNotFoundException("Option not found"));
        ownerAuthHelper.verifyAll(menuOption.getMenu().getCategory().getStore(), ctx);

        menuOptionRepository.delete(menuOption);


    }


}
