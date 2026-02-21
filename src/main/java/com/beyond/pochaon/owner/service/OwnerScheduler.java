package com.beyond.pochaon.owner.service;


import com.beyond.pochaon.ordering.repository.OrderingRepository;
import com.beyond.pochaon.store.domain.Store;
import com.beyond.pochaon.store.domain.StoreSettlement;
import com.beyond.pochaon.store.repository.StoreRepository;
import com.beyond.pochaon.store.repository.StoreSettlementRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@Transactional
public class OwnerScheduler {
    private final StoreRepository storeRepository;
    private final TaskScheduler taskScheduler;
    private final OrderingRepository orderingRepository;
    private final StoreSettlementRepository storeSettlementRepository;

    @Autowired
    public OwnerScheduler(StoreRepository storeRepository, @Qualifier("taskSchedular")
    TaskScheduler taskScheduler, OrderingRepository orderingRepository, StoreSettlementRepository storeSettlementRepository) {
        this.storeRepository = storeRepository;
        this.taskScheduler = taskScheduler;
        this.orderingRepository = orderingRepository;
        this.storeSettlementRepository = storeSettlementRepository;
    }

    @Scheduled(cron = "0 0 15 * * *")
//    서버가 2대여도 store_@@키를 가진 서버 하나만 실행됨
//    lockAtLeastFor: 작업이 끝나더라도 최소 1분동안은 키 반납을 안함(서버 초차때문에)
//   lockAtMostFor: 작업이 길어지고 서버가 고장나도 5분 안에는 무조건 열쇠를 해제하라는 설정

    @SchedulerLock(name = "store_schedular_lock", lockAtLeastFor = "1m", lockAtMostFor = "5m")
    public void storeScheduler() {
        log.info("==== 마감 정산 예약 스케줄러 시작 ====");
        List<Store> stores = storeRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        for (Store store : stores) {
            LocalTime closedAt = store.getStoreCloseAt();

            if (closedAt == null) {
                log.warn("매장ID {} 마감시간이 null → 스케줄링 스킵", store.getId());
                continue;
            }

//            오늘 날짜와 마감시간을 합침
            LocalDateTime targetDateTime = LocalDateTime.of(LocalDate.now(), closedAt);
//            다음날 새벽 마감인경우
            if (targetDateTime.isBefore(now)) {
                targetDateTime = targetDateTime.plusDays(1);
            }
            log.info("매장ID: {}, 예약시간: {}", store.getId(), targetDateTime);
//            tarrgetDateTime에 settlement가 동작하도록 예약
            taskScheduler.schedule(() -> settlement(store.getId()),
                    targetDateTime.atZone(ZoneId.systemDefault()).toInstant());
        }
    }

    @Transactional
    public void settlement(Long storeId) {
        log.info("==== 매장ID: {} 정산 로직 실행 ====", storeId);
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new EntityNotFoundException("없는 매장입니다. owner_ser_settlement"));

//        영업 시작,마감 시간
        LocalTime openTime = store.getStoreOpenAt();
        LocalTime closeTime = store.getStoreCloseAt();

//        현재 정산 시점(마감시간)
        LocalDateTime currentClosedAt = LocalDateTime.of(LocalDate.now(), closeTime);
//        오늘 날짜(연,월,일) + 오픈시간(시,분)
        LocalDateTime currentOpenedAt = LocalDateTime.of(LocalDate.now(), openTime);

//        영업 시작 시간(오픈 시간)이 현재 시간(마감 시점)보다 더 늦는다면
        if (openTime.isAfter(currentOpenedAt.toLocalTime())) {
            currentOpenedAt = currentOpenedAt.minusDays(1);
        }

        log.info("==정산 프로세스 시작 ==");
        log.info("매장명: {}", store.getStoreName());
        log.info("영업 시작: {}", currentOpenedAt);
        log.info("영업 종료(현재): {}", currentClosedAt);

//        매출 합계 조회
        int dayTotalAmount = orderingRepository.sumTotalRevenue(storeId, currentOpenedAt, currentClosedAt);

        int orderCount = orderingRepository.countCompletedOrders(store.getId(), currentOpenedAt, currentClosedAt);

        int averageOrderAmount = 0;

        if (dayTotalAmount <= 0) dayTotalAmount = 0;
        if (orderCount <= 0) {
            orderCount = 0;
        } else {
            averageOrderAmount = dayTotalAmount / orderCount;
        }


        log.info("최종 집계 매출: {}원", dayTotalAmount);
        log.info("주문 건수: {}건", orderCount);
        log.info("평균 단가: {}원", averageOrderAmount);


//        정산 데이터 저장
        StoreSettlement settlement = StoreSettlement.builder()
                .dayTotalAmount(dayTotalAmount)
                .orderCount(orderCount)
                .averageOrderAmount(averageOrderAmount)
                .store(store)
                .build();
        storeSettlementRepository.save(settlement);

        log.info("==== 매장ID: {} 정산 완료 ====", storeId);

    }

}
