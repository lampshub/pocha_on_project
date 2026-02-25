package com.beyond.pochaon.owner.service;


import com.beyond.pochaon.ordering.repository.OrderingRepository;
import com.beyond.pochaon.payment.repository.PaymentRepository;
import com.beyond.pochaon.store.domain.Store;
import com.beyond.pochaon.store.domain.StoreSettlement;
import com.beyond.pochaon.store.repository.StoreRepository;
import com.beyond.pochaon.store.repository.StoreSettlementRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@Transactional
public class OwnerScheduler {
    private final StoreRepository storeRepository;
    private final OrderingRepository orderingRepository;
    private final StoreSettlementRepository storeSettlementRepository;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final PaymentRepository paymentRepository;

    @Autowired
    public OwnerScheduler(StoreRepository storeRepository, OrderingRepository orderingRepository, StoreSettlementRepository storeSettlementRepository, @Qualifier("taskSchedular") ThreadPoolTaskScheduler taskScheduler, PaymentRepository paymentRepository) {
        this.storeRepository = storeRepository;
        this.orderingRepository = orderingRepository;
        this.storeSettlementRepository = storeSettlementRepository;
        this.taskScheduler = taskScheduler;
        this.paymentRepository = paymentRepository;
    }


    //    서버가 2대여도 store_@@키를 가진 서버 하나만 실행됨
//    lockAtLeastFor: 작업이 끝나더라도 최소 1분동안은 키 반납을 안함(서버 초차때문에)
//   lockAtMostFor: 작업이 길어지고 서버가 고장나도 5분 안에는 무조건 열쇠를 해제하라는 설정
//    @Scheduled(cron = "0 0 15 * * *") // 실제 배포시
//    @Scheduled(cron = "0 */5 * * * *") //테스트용
//    @SchedulerLock(name = "scheduleSettlement", lockAtLeastFor = "1m", lockAtMostFor = "PT30M")
//    public void storeScheduler() {
//        log.info("==== 정산 예약 스케줄러 시작 ====");
//        List<Store> stores = storeRepository.findAll();
//        LocalDateTime now = LocalDateTime.now();
//
//        for (Store store : stores) {
//            LocalTime closedAt = store.getStoreCloseAt();
//            if (closedAt == null) {
//                log.warn("매장ID {} 마감시간이 null → 스케줄링 스킵", store.getId());
//                continue;
//            }
//
////            오늘 날짜와 마감시간을 합침
//            LocalDateTime targetDateTime = LocalDateTime.of(LocalDate.now(), closedAt);
////            다음날 새벽 마감인경우
//            if (targetDateTime.isBefore(now)) {
//                targetDateTime = targetDateTime.plusDays(1);
//            }
//            log.info("매장ID: {}, 예약시간: {}", store.getId(), targetDateTime);

    /// /            tarrgetDateTime에 settlement가 동작하도록 예약
//            taskScheduler.schedule(() -> settlement(store.getId()),
//                    targetDateTime.atZone(ZoneId.systemDefault()).toInstant());
//        }
//    }
    @Scheduled(cron = "0 */3 * * * *")
    @SchedulerLock(name = "scheduleSettlement", lockAtLeastFor = "1m", lockAtMostFor = "PT30M")
    public void storeScheduler() {
        log.info("==== 정산 예약 스케줄러 시작 ====");
        List<Store> stores = storeRepository.findAll();

        for (Store store : stores) {
            if (store.getStoreCloseAt() == null) {
                log.warn("매장ID {} 마감시간이 null → 스케줄링 스킵", store.getId());
                continue;
            }
            log.info("매장ID: {} 즉시 정산 실행 (테스트)", store.getId());
            settlement(store.getId());  // ✅ 예약 없이 바로 실행
        }
    }

    @Transactional
    public void settlement(Long storeId) {
        log.info("==== 매장ID: {} 정산 로직 실행 ====", storeId);
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("없는 매장입니다."));

        LocalTime openTime = store.getStoreOpenAt();
        LocalDateTime now = LocalDateTime.now();

        // 영업 종료 = 정산 실행 시점(현재)
        LocalDateTime currentClosedAt = now;

        // 영업 시작 = 오늘 오픈시간
        LocalDateTime currentOpenedAt = LocalDateTime.of(LocalDate.now(), openTime);

        // 오픈시간이 현재보다 미래라면 → 어제 오픈한 것 (새벽 영업 케이스)
        if (currentOpenedAt.isAfter(now)) {
            currentOpenedAt = currentOpenedAt.minusDays(1);
        }

        log.info("==정산 프로세스 시작 ==");
        log.info("매장명: {}", store.getStoreName());
        log.info("영업 시작: {}", currentOpenedAt);
        log.info("영업 종료(현재): {}", currentClosedAt);

        // 오늘 날짜 기준 기존 정산 레코드 조회 (중복 insert 방지)
        LocalDate today = LocalDate.now();
        List<StoreSettlement> existingList = storeSettlementRepository
                .findByStoreAndSettlementDate(store, today);
        StoreSettlement existing = null;
        if (!existingList.isEmpty()) {
            // 최신 것 하나만 남기고 나머지 삭제
            existingList.sort((a, b) -> b.getStoreSettlementId().compareTo(a.getStoreSettlementId()));
            existing = existingList.get(0);
            if (existingList.size() > 1) {
                List<StoreSettlement> duplicates = existingList.subList(1, existingList.size());
                storeSettlementRepository.deleteAll(duplicates);
                log.warn("중복 정산 레코드 {}건 삭제", duplicates.size());
            }
        }

        int dayTotalAmount = 0;
        Integer rawRevenue = orderingRepository.sumTotalRevenue(storeId, currentOpenedAt, currentClosedAt);
        if (rawRevenue != null && rawRevenue > 0) {
            dayTotalAmount = rawRevenue;
        }

        int orderCount = orderingRepository.countCompletedOrders(storeId, currentOpenedAt, currentClosedAt);
        int averageOrderAmount = orderCount > 0 ? dayTotalAmount / orderCount : 0;
        int cancelCount = orderingRepository.countCancelledOrders(storeId, currentOpenedAt, currentClosedAt);
        int tableUseCount = orderingRepository.countDistinctGroupIds(storeId, currentOpenedAt, currentClosedAt);

        int cardSales = 0, cashSales = 0, transferSales = 0;
        try {
            cardSales = paymentRepository.sumCardSales(storeId, currentOpenedAt, currentClosedAt);
            cashSales = paymentRepository.sumCashSales(storeId, currentOpenedAt, currentClosedAt);
            transferSales = paymentRepository.sumTransferSales(storeId, currentOpenedAt, currentClosedAt);
        } catch (Exception e) {
            log.warn("결제 수단별 매출 집계 실패(매장Id: {}): {}", storeId, e.getMessage());
        }

        int refundAmount = 0;
        int netSales = dayTotalAmount - refundAmount;

        log.info("최종 집계 - 매출: {}원, 주문: {}건, 취소: {}건", dayTotalAmount, orderCount, cancelCount);
        log.info("카드: {}원, 현금: {}원, 이체: {}원", cardSales, cashSales, transferSales);
        log.info("테이블이용: {}회, 순매출: {}원", tableUseCount, netSales);

        if (existing != null) {
            // 기존 레코드 업데이트 (같은 날 중복 insert 방지)
            StoreSettlement updated = StoreSettlement.builder()
                    .storeSettlementId(existing.getStoreSettlementId())
                    .settlementDate(today)
                    .dayTotalAmount(dayTotalAmount)
                    .orderCount(orderCount)
                    .averageOrderAmount(averageOrderAmount)
                    .cancelCount(cancelCount)
                    .refundAmount(refundAmount)
                    .netSales(netSales)
                    .cardSales(cardSales)
                    .cashSales(cashSales)
                    .transferSales(transferSales)
                    .tableUseCount(tableUseCount)
                    .store(store)
                    .build();
            storeSettlementRepository.save(updated);
            log.info("==== 매장ID: {} 정산 업데이트 완료 ====", storeId);
        } else {
            StoreSettlement settlement = StoreSettlement.builder()
                    .settlementDate(today)
                    .dayTotalAmount(dayTotalAmount)
                    .orderCount(orderCount)
                    .averageOrderAmount(averageOrderAmount)
                    .cancelCount(cancelCount)
                    .refundAmount(refundAmount)
                    .netSales(netSales)
                    .cardSales(cardSales)
                    .cashSales(cashSales)
                    .transferSales(transferSales)
                    .tableUseCount(tableUseCount)
                    .store(store)
                    .build();
            storeSettlementRepository.save(settlement);
            log.info("==== 매장ID: {} 정산 신규 저장 완료 ====", storeId);
        }
    }
}
