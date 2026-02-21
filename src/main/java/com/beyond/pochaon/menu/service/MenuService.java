package com.beyond.pochaon.menu.service;


import com.beyond.pochaon.menu.domain.Category;
import com.beyond.pochaon.menu.domain.Menu;
import com.beyond.pochaon.menu.dtos.MenuCreateReqDto;
import com.beyond.pochaon.menu.dtos.MenuUpdateReqDto;
import com.beyond.pochaon.menu.repository.CategoryRepository;
import com.beyond.pochaon.menu.repository.MenuRepository;
import com.beyond.pochaon.owner.domain.Owner;
import com.beyond.pochaon.owner.repository.OwnerRepository;
import com.beyond.pochaon.store.domain.Store;
import com.beyond.pochaon.store.repository.StoreRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.AccessDeniedException;

@Service
@Transactional
public class MenuService {
    private final MenuRepository menuRepository;
    private final OwnerRepository ownerRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;
    private final HttpServletRequest request;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket1}")
    private String bucket;

    @Autowired
    public MenuService(MenuRepository menuRepository, OwnerRepository ownerRepository, StoreRepository storeRepository, CategoryRepository categoryRepository, HttpServletRequest request, S3Client s3Client) {
        this.menuRepository = menuRepository;
        this.ownerRepository = ownerRepository;
        this.storeRepository = storeRepository;
        this.categoryRepository = categoryRepository;
        this.request = request;
        this.s3Client = s3Client;
    }

    //    owner 메뉴 추가
    public Long createMenu(MenuCreateReqDto reqDto) throws AccessDeniedException {
        String email = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Owner owner = ownerRepository.findByOwnerEmail(email).orElseThrow(() -> new EntityNotFoundException("Owner not found"));

        Long storeId = (Long) request.getAttribute("storeId");
        if (storeId == null) {
            throw new AccessDeniedException("해당 권한이 없습니다");
        }
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new EntityNotFoundException("Store not found"));
        if (!store.getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("해당 권한이 없습니다");
        }

        Category category = categoryRepository.findById(reqDto.getCategoryId()).orElseThrow(() -> new EntityNotFoundException("Category not found"));
        Menu menu = menuRepository.save(reqDto.toEntity(store, category));
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
            menu.updateMenuImageUrl(imgUrl);    //S3에 업로드한 이미지URL 저장
        }
        return menu.getId();
    }


    //    owner 메뉴 수정
    public void updateMenu(Long menuId, MenuUpdateReqDto reqDto) throws AccessDeniedException {
        String email = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Owner owner = ownerRepository.findByOwnerEmail(email).orElseThrow(() -> new EntityNotFoundException("Owner not found"));

        Long storeId = (Long) request.getAttribute("storeId");
        if (storeId == null) {
            throw new AccessDeniedException("해당 권한이 없습니다");
        }
        Menu menu = menuRepository.findById(menuId).orElseThrow(() -> new EntityNotFoundException("Menu not found"));
        if (!menu.getStore().getId().equals(storeId)) {
            throw new AccessDeniedException("로그인된 매장의 메뉴가 아닙니다");
        }
        if (!menu.getStore().getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("해당 권한이 없습니다");
        }
        Category category = menu.getCategory();
        if (reqDto.getCategoryId() != null && !reqDto.getCategoryId().equals(category.getId())) {
            category = categoryRepository.findById(reqDto.getCategoryId()).orElseThrow(() -> new EntityNotFoundException("Category not found"));
        }

        menu.update(
                reqDto.getMenuName(),
                reqDto.getPrice(),
                reqDto.getOrigin(),
                reqDto.getExplanation(),
                category
        );

        if (reqDto.getMenuImage() != null && !reqDto.getMenuImage().isEmpty()) {
            //기존 이미지가 null이 아니면 삭제
            if (menu.getMenuImageUrl() != null) {
                String imgUrl = menu.getMenuImageUrl();
                String fileName = imgUrl.substring(imgUrl.lastIndexOf("/") + 1);
                s3Client.deleteObject(a -> a.bucket(bucket).key(fileName));
            }
//            신규이미지 등록
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
        } else if (Boolean.TRUE.equals(reqDto.getDeleteImage())){
//            이미지를 삭제 : deleteImage true로 설정
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
        String email = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Owner owner = ownerRepository.findByOwnerEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Owner not found"));

        Long storeId = (Long) request.getAttribute("storeId");
        if (storeId == null) {
            throw new AccessDeniedException("해당 권한이 없습니다");
        }
        Menu menu = menuRepository.findById(menuId).orElseThrow(() -> new EntityNotFoundException("Menu not found"));
        if (!menu.getStore().getId().equals(storeId)) {
            throw new AccessDeniedException("로그인된 매장의 메뉴가 아닙니다");
        }
        if (!menu.getStore().getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("해당 권한이 없습니다");
        }
        // 이미지 삭제
        if (menu.getMenuImageUrl() != null) {
            String imgUrl = menu.getMenuImageUrl();
            String fileName = imgUrl.substring(imgUrl.lastIndexOf("/")+1);
            s3Client.deleteObject(a -> a.bucket(bucket).key(fileName));
        }
        // 메뉴 삭제
        menuRepository.delete(menu);
    }
}