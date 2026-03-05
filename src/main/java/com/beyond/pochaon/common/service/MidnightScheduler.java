package com.beyond.pochaon.common.service;

import com.beyond.pochaon.ingredient.service.IngredientService;
import com.beyond.pochaon.store.domain.Store;
import com.beyond.pochaon.store.repository.StoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
public class MidnightScheduler {

    private final IngredientService ingredientService;
    private final StoreRepository storeRepository;
    @Autowired
    public MidnightScheduler(IngredientService ingredientService, StoreRepository storeRepository) {
        this.ingredientService = ingredientService;
        this.storeRepository = storeRepository;
    }

//     초 분 시 일 월 요일
    @Scheduled(cron = "0 0 0 * * *")
    public void runMidnightTasks() {
        log.info("자정 스케줄러 실행 시작");
        LocalDate today = LocalDate.now();
        List<Store> autoRenewStore = storeRepository.findByAutoRenewTrueAndServiceEndAt(today);
        for(Store store : autoRenewStore){
            try {
                store.extendOneMonth();
                log.info("서비스 이용기간 1달 연장 자동저장 완료 : storeId: {}, 새 만료일: {}", store.getId(), store.getServiceEndAt());
                log.info("지니 작업 완료");
            } catch (Exception e){
                log.error("서비스 이용기간 자동연장 실패 : storeId: {}", store.getId(), e);
                log.error("지니 로직 실행 중 에러 발생: {}", e.getMessage());
            }
        }

        try {
//             만료 재고 정리 (모든 매장 대상 등)
             ingredientService.cleanupExpiredIngredientsForAll();
            log.info("만료 재고 정리 완료");
        } catch (Exception e) {
            log.error("내 로직 실행 중 에러 발생: {}", e.getMessage());
        }

        log.info("자정 스케줄러 실행 종료");
    }
}
