package com.beyond.pochaon.store.common;

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
public class StoreAutoRenewScheduler {
    private final StoreRepository storeRepository;
    @Autowired
    public StoreAutoRenewScheduler(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void autoRenew(){
        LocalDate today = LocalDate.now();
        List<Store> autoRenewStore = storeRepository.findByAutoRenewTrueAndServiceEndAt(today);

        for(Store store : autoRenewStore){
            try {
                store.extendOneMonth();
                log.info("서비스 이용기간 1달 연장 자동저장 완료 : storeId: {}, 새 만료일: {}", store.getId(), store.getServiceEndAt());
            } catch (Exception e){
                log.error("서비스 이용기간 자동연장 실패 : storeId: {}", store.getId(), e);
            }
        }
    }
}
