package com.beyond.pochaon.menu.service;


import com.beyond.pochaon.common.auth.OwnerAuthHelper;
import com.beyond.pochaon.common.auth.OwnerAuthHelper.OwnerContext;
import com.beyond.pochaon.menu.domain.Category;
import com.beyond.pochaon.menu.domain.Menu;
import com.beyond.pochaon.menu.domain.MenuOption;
import com.beyond.pochaon.menu.domain.MenuOptionDetail;
import com.beyond.pochaon.menu.dto.MenuCreateReqDto;
import com.beyond.pochaon.menu.dto.MenuResToOwnerDto;
import com.beyond.pochaon.menu.dto.MenuUpdateReqDto;
import com.beyond.pochaon.menu.repository.CategoryRepository;
import com.beyond.pochaon.menu.repository.MenuRepository;
import com.beyond.pochaon.store.domain.Store;
import com.beyond.pochaon.store.repository.StoreRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class MenuService {
    private final MenuRepository menuRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;
    private final S3Client s3Client;
    private final OwnerAuthHelper ownerAuthHelper;

    @Value("${aws.s3.bucket1}")
    private String bucket;

    @Autowired
    public MenuService(MenuRepository menuRepository, StoreRepository storeRepository, CategoryRepository categoryRepository,S3Client s3Client, OwnerAuthHelper ownerAuthHelper) {
        this.menuRepository = menuRepository;
        this.storeRepository = storeRepository;
        this.categoryRepository = categoryRepository;
        this.s3Client = s3Client;
        this.ownerAuthHelper = ownerAuthHelper;
    }

    //    owner 메뉴 추가
    public Long createMenu(MenuCreateReqDto reqDto) throws AccessDeniedException {
        OwnerContext ctx = ownerAuthHelper.getOwnerContext();
        Store store = storeRepository.findById(ctx.getStoreId())
                .orElseThrow(() -> new EntityNotFoundException("Store not found"));
        ownerAuthHelper.verifyStoreOwnerShip(store, ctx.getOwner());

        Category category = categoryRepository.findById(reqDto.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));
        Menu menu = menuRepository.save(reqDto.toEntity(category));

        if (reqDto.getMenuImage() != null && !reqDto.getMenuImage().isEmpty()) {
            String fileName = "menu-" + menu.getId() + "-" + reqDto.getMenuImage().getOriginalFilename();
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .contentType(reqDto.getMenuImage().getContentType())
                    .build();
            try {
                s3Client.putObject(putObjectRequest, RequestBody.fromBytes(reqDto.getMenuImage().getBytes()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String imgUrl = s3Client.utilities().getUrl(a -> a.bucket(bucket).key(fileName)).toExternalForm();
            menu.updateMenuImageUrl(imgUrl);
        }
        return menu.getId();
    }



    //    owner 메뉴 수정
    public void updateMenu(Long menuId, MenuUpdateReqDto reqDto) throws AccessDeniedException {
        OwnerContext ctx = ownerAuthHelper.getOwnerContext();
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new EntityNotFoundException("Menu not found"));
        ownerAuthHelper.verifyAll(menu.getCategory().getStore(), ctx);

        Category category = menu.getCategory();
        if (reqDto.getCategoryId() != null && !reqDto.getCategoryId().equals(category.getId())) {
            category = categoryRepository.findById(reqDto.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found"));
        }

        menu.update(
                reqDto.getMenuName(),
                reqDto.getPrice(),
                reqDto.getOrigin(),
                reqDto.getExplanation(),
                category
        );

        if (reqDto.getMenuImage() != null && !reqDto.getMenuImage().isEmpty()) {
//            기존 이미지가 null이 아닐경우 삭제
            if (menu.getMenuImageUrl() != null) {
                String imgUrl = menu.getMenuImageUrl();
                String fileName = imgUrl.substring(imgUrl.lastIndexOf("/") + 1);
                s3Client.deleteObject(a -> a.bucket(bucket).key(fileName));
            }
//            이미지 등록
            String newFileName = "menu-" + menu.getId() + "-" + reqDto.getMenuImage().getOriginalFilename();
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(newFileName)
                    .contentType(reqDto.getMenuImage().getContentType())
                    .build();
            try {
                s3Client.putObject(putObjectRequest, RequestBody.fromBytes(reqDto.getMenuImage().getBytes()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String newImgUrl = s3Client.utilities().getUrl(a -> a.bucket(bucket).key(newFileName)).toExternalForm();
            menu.updateMenuImageUrl(newImgUrl);
        } else if (Boolean.TRUE.equals(reqDto.getDeleteImage())) {
//          기존 이미지 삭제
            if (menu.getMenuImageUrl() != null) {
                String imgUrl = menu.getMenuImageUrl();
                String fileName = imgUrl.substring(imgUrl.lastIndexOf("/") + 1);
                s3Client.deleteObject(a -> a.bucket(bucket).key(fileName));
                menu.updateMenuImageUrl(null);
            }
        }
    }


    //    owner 메뉴 삭제
    public void deleteMenu(Long menuId) throws AccessDeniedException {
        OwnerContext ctx = ownerAuthHelper.getOwnerContext();
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new EntityNotFoundException("Menu not found"));
        ownerAuthHelper.verifyAll(menu.getCategory().getStore(), ctx);

        if (menu.getMenuImageUrl() != null) {
            String imgUrl = menu.getMenuImageUrl();
            String fileName = imgUrl.substring(imgUrl.lastIndexOf("/") + 1);
            s3Client.deleteObject(a -> a.bucket(bucket).key(fileName));
        }
        menuRepository.delete(menu);
    }


    @Transactional(readOnly = true)
//    메뉴 상세 조회 (점주 메뉴 수정용)
    public MenuResToOwnerDto getMenuDetail(Long menuId) throws AccessDeniedException {
        OwnerContext ctx = ownerAuthHelper.getOwnerContext();
        Menu menu = menuRepository.findDetailById(menuId)
                .orElseThrow(() -> new EntityNotFoundException("Menu not found"));

        List<MenuResToOwnerDto.OptionDto> optionDtos = new ArrayList<>();
        for (MenuOption option : menu.getMenuOptionList()) {
            List<MenuResToOwnerDto.OptionDetailDto> detailDtos = new ArrayList<>();
            for (MenuOptionDetail detail : option.getMenuOptionDetailList()) {
                detailDtos.add(MenuResToOwnerDto.OptionDetailDto.builder()
                        .optionDetailId(detail.getId())
                        .optionDetailName(detail.getOptionDetailName())
                        .optionDetailPrice(detail.getOptionDetailPrice())
                        .maxQuantity(detail.getMaxQuantity())
                        .build());
            }
            optionDtos.add(MenuResToOwnerDto.OptionDto.builder()
                    .optionId(option.getId())
                    .optionName(option.getOptionName())
                    .selectionType(option.getSelectionType())
                    .minSelect(option.getMinSelect())
                    .maxSelect(option.getMaxSelect())
                    .details(detailDtos)
                    .build());
        }

        return MenuResToOwnerDto.builder()
                .menuName(menu.getMenuName())
                .price(menu.getPrice())
                .origin(menu.getOrigin())
                .explanation(menu.getExplanation())
                .imageUrl(menu.getMenuImageUrl())
                .categoryId(menu.getCategory().getId())
                .categoryName(menu.getCategory().getCategoryName())
                .options(optionDtos)
                .build();
    }
}