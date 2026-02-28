//package com.beyond.pochaon.owner.service;
//
//import com.beyond.pochaon.ordering.domain.Ordering;
//import com.beyond.pochaon.ordering.domain.OrderingDetail;
//import com.beyond.pochaon.ordering.repository.OrderingRepository;
//import com.beyond.pochaon.payment.repository.PaymentRepository;
//import com.beyond.pochaon.store.domain.*;
//import com.beyond.pochaon.store.repository.StoreRepository;
//import com.beyond.pochaon.store.repository.StoreSettlementRepository;
//import jakarta.persistence.EntityNotFoundException;
//import lombok.extern.slf4j.Slf4j;
//import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.*;
//import java.util.*;
//
//@Slf4j
//@Component
//public class OwnerScheduler {
//
//    private final StoreRepository storeRepository;
//    private final OrderingRepository orderingRepository;
//    private final StoreSettlementRepository storeSettlementRepository;
//    private final ThreadPoolTaskScheduler taskScheduler;
//    private final PaymentRepository paymentRepository;
//
//    @Autowired
//    public OwnerScheduler(StoreRepository storeRepository,
//                          OrderingRepository orderingRepository,
//                          StoreSettlementRepository storeSettlementRepository,
//                          @Qualifier("taskSchedular") ThreadPoolTaskScheduler taskScheduler,
//                          PaymentRepository paymentRepository) {
//        this.storeRepository = storeRepository;
//        this.orderingRepository = orderingRepository;
//        this.storeSettlementRepository = storeSettlementRepository;
//        this.taskScheduler = taskScheduler;
//        this.paymentRepository = paymentRepository;
//    }
//
//    // ═══════════════════════════════════════════════════════════
//    //  스케줄러 진입점
//    //  - 매일 오후 3시에 실행
//    //  - 모든 매장을 조회해서 각 매장의 마감시간에 맞춰 정산 예약
//    //  - ShedLock으로 서버 2대여도 1대만 실행
//    // ═══════════════════════════════════════════════════════════
//
//    @Scheduled(cron = "0 0 15 * * *")
//    @SchedulerLock(name = "scheduleSettlement", lockAtLeastFor = "1m", lockAtMostFor = "PT30M")
//    public void storeScheduler() {
//        log.info("==== 정산 예약 스케줄러 시작 ====");
//        List<Store> stores = storeRepository.findAll();
//        LocalDateTime now = LocalDateTime.now();
//
//        // 모든 매장을 돌면서 각 매장의 마감시간에 settlement()가 실행되도록 예약
//        for (Store store : stores) {
//            LocalTime closedAt = store.getStoreCloseAt();
//            if (closedAt == null) {
//                log.warn("매장ID {} 마감시간이 null → 스케줄링 스킵", store.getId());
//                continue;
//            }
//
//            // 오늘 날짜 + 마감시간 = 정산 실행 예정 시각
//            LocalDateTime targetDateTime = LocalDateTime.of(LocalDate.now(), closedAt);
//            // 이미 지난 시간이면 내일로 예약 (새벽 마감 케이스)
//            if (targetDateTime.isBefore(now)) {
//                targetDateTime = targetDateTime.plusDays(1);
//            }
//
//            log.info("매장ID: {}, 예약시간: {}", store.getId(), targetDateTime);
//
//            // taskScheduler로 해당 시각에 settlement() 실행 예약
//            taskScheduler.schedule(() -> settlement(store.getId()),
//                    targetDateTime.atZone(ZoneId.systemDefault()).toInstant());
//        }
//        log.info("==== 정산 예약 스케줄러 완료 ====");
//    }
//
//    // ═══════════════════════════════════════════════════════════
//    //  핵심 정산 로직
//    //  - 매장 1개에 대해 오늘 영업시간 동안의 데이터를 집계
//    //  - StoreSettlement(부모) + 자식 엔티티 3개에 저장
//    // ═══════════════════════════════════════════════════════════
//
//    @Transactional
//    public void settlement(Long storeId) {
//        log.info("── 매장ID: {} 정산 시작 ──", storeId);
//
//        Store store = storeRepository.findById(storeId)
//                .orElseThrow(() -> new EntityNotFoundException("없는 매장입니다."));
//
//        // ── 영업 시간 범위 계산 ──
//        // 오픈시간이 현재보다 미래면 어제 오픈한 것 (새벽 영업 케이스)
//        LocalTime openTime = store.getStoreOpenAt();
//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime currentClosedAt = now;  // 정산 실행 시점 = 영업 종료
//        LocalDateTime currentOpenedAt = LocalDateTime.of(LocalDate.now(), openTime);
//        if (currentOpenedAt.isAfter(now)) {
//            currentOpenedAt = currentOpenedAt.minusDays(1);
//        }
//
//        LocalDate today = LocalDate.now();
//        log.info("매장명: {}, 영업: {} ~ {}", store.getStoreName(), currentOpenedAt, currentClosedAt);
//
//        // ══════════════════════════════════════════
//        // 1단계: 기본 매출 집계 (Repository 쿼리로 한번에)
//        // ══════════════════════════════════════════
//
//        int dayTotalAmount = 0;
//        Integer rawRevenue = orderingRepository.sumTotalRevenue(storeId, currentOpenedAt, currentClosedAt);
//        if (rawRevenue != null && rawRevenue > 0) {
//            dayTotalAmount = rawRevenue;
//        }
//
//        int orderCount = orderingRepository.countCompletedOrders(storeId, currentOpenedAt, currentClosedAt);
//        int averageOrderAmount = orderCount > 0 ? dayTotalAmount / orderCount : 0;
//        int cancelCount = orderingRepository.countCancelledOrders(storeId, currentOpenedAt, currentClosedAt);
//        int tableUseCount = orderingRepository.countDistinctGroupIds(storeId, currentOpenedAt, currentClosedAt);
//        int refundAmount = 0;
//        int netSales = dayTotalAmount - refundAmount;
//
//        // ══════════════════════════════════════════
//        // 2단계: 결제수단별 집계
//        // - sumByPaymentMethod()가 [method, amount, count] 배열 리스트를 반환
//        // - for문으로 돌면서 method 이름에 따라 각 변수에 할당
//        //
//        // 예시 데이터:
//        //   [["카드", 50000, 3], ["간편결제", 20000, 1]]
//        //   → cardAmount=50000, cardCount=3
//        //   → easyPayAmount=20000, easyPayCount=1
//        // ══════════════════════════════════════════
//
//        int cardAmount = 0, cardCount = 0;
//        int transferAmount = 0, transferCount = 0;
//        int easyPayAmount = 0, easyPayCount = 0;
//        int phoneAmount = 0, phoneCount = 0;
//
//        try {
//            List<Object[]> methodRaw = paymentRepository.sumByPaymentMethod(storeId, currentOpenedAt, currentClosedAt);
//
//            for (Object[] row : methodRaw) {
//                String method = (String) row[0];
//                int amount = ((Number) row[1]).intValue();
//                int count = ((Number) row[2]).intValue();
//                if (method == null) continue;
//
//                switch (method) {
//                    case "카드" -> { cardAmount = amount; cardCount = count; }
//                    case "계좌이체" -> { transferAmount = amount; transferCount = count; }
//                    case "간편결제" -> { easyPayAmount = amount; easyPayCount = count; }
//                    case "휴대폰" -> { phoneAmount = amount; phoneCount = count; }
//                }
//            }
//        } catch (Exception e) {
//            log.warn("결제수단 집계 실패(매장ID: {}): {}", storeId, e.getMessage());
//        }
//
//        // ══════════════════════════════════════════
//        // 3단계: 메뉴별 / 카테고리별 집계
//        // - DB에서 주문 목록을 가져와서 자바에서 for문으로 집계
//        // - DB 쿼리 하나로 못하는 이유:
//        //   주문 1건 안에 메뉴가 여러 개이고,
//        //   메뉴별로 수량/금액을 합산한 뒤 순위까지 매겨야 하기 때문
//        // ══════════════════════════════════════════
//
//        List<Ordering> completedOrders = orderingRepository
//                .findCompletedOrdersWithDetails(storeId, currentOpenedAt, currentClosedAt);
//
//        List<SettlementMenuRank> menuRanks = buildMenuRanks(completedOrders);
//        List<SettlementCategoryRank> categoryRanks = buildCategoryRanks(completedOrders);
//
//        // ══════════════════════════════════════════
//        // 4단계: 테이블별 집계
//        // - 테이블별 매출/주문수와 이용횟수를 각각 다른 쿼리로 가져와서
//        //   테이블번호 기준으로 합침
//        // ══════════════════════════════════════════
//
//        List<SettlementTableStat> tableStats = buildTableStats(storeId, currentOpenedAt, currentClosedAt);
//
//        // ══════════════════════════════════════════
//        // 5단계: DB 저장 (upsert)
//        // - 같은 날짜에 이미 정산 데이터가 있으면 → update + 자식 교체
//        // - 없으면 → 신규 생성
//        // - 자식 엔티티는 clearChildren() 후 다시 추가
//        //   (orphanRemoval=true라서 clear하면 기존 DB row 자동 삭제)
//        // ══════════════════════════════════════════
//
//        Optional<StoreSettlement> existingOpt = storeSettlementRepository
//                .findByStoreAndSettlementDate(store, today);
//
//        StoreSettlement settlement;
//
//        if (existingOpt.isPresent()) {
//            // 기존 레코드 업데이트
//            settlement = existingOpt.get();
//            settlement.update(
//                    dayTotalAmount, orderCount, averageOrderAmount,
//                    cancelCount, refundAmount, netSales,
//                    cardAmount, cardCount, transferAmount, transferCount,
//                    easyPayAmount, easyPayCount, phoneAmount, phoneCount,
//                    tableUseCount
//            );
//            // 자식 엔티티 전부 삭제 후 다시 추가
//            settlement.clearChildren();
//            storeSettlementRepository.saveAndFlush(settlement);
//        } else {
//            // 신규 생성
//            settlement = StoreSettlement.builder()
//                    .store(store)
//                    .settlementDate(today)
//                    .dayTotalAmount(dayTotalAmount)
//                    .orderCount(orderCount)
//                    .averageOrderAmount(averageOrderAmount)
//                    .cancelCount(cancelCount)
//                    .refundAmount(refundAmount)
//                    .netSales(netSales)
//                    .cardAmount(cardAmount).cardCount(cardCount)
//                    .transferAmount(transferAmount).transferCount(transferCount)
//                    .easyPayAmount(easyPayAmount).easyPayCount(easyPayCount)
//                    .phoneAmount(phoneAmount).phoneCount(phoneCount)
//                    .tableUseCount(tableUseCount)
//                    .build();
//        }
//
//        // ── 자식 엔티티 연결 ──
//        // 각 리스트를 돌면서 부모(settlement)와 연결해서 추가
//        // cascade=ALL이라 settlement 저장할 때 자식도 같이 INSERT됨
//
//        for (SettlementMenuRank mr : menuRanks) {
//            settlement.addMenuRank(SettlementMenuRank.builder()
//                    .settlement(settlement)
//                    .menuId(mr.getMenuId())
//                    .menuName(mr.getMenuName())
//                    .categoryName(mr.getCategoryName())
//                    .salesCount(mr.getSalesCount())
//                    .salesAmount(mr.getSalesAmount())
//                    .rankByCount(mr.getRankByCount())
//                    .rankByAmount(mr.getRankByAmount())
//                    .build());
//        }
//
//        for (SettlementCategoryRank cr : categoryRanks) {
//            settlement.addCategoryRank(SettlementCategoryRank.builder()
//                    .settlement(settlement)
//                    .categoryId(cr.getCategoryId())
//                    .categoryName(cr.getCategoryName())
//                    .salesCount(cr.getSalesCount())
//                    .salesAmount(cr.getSalesAmount())
//                    .rankByAmount(cr.getRankByAmount())
//                    .build());
//        }
//
//        for (SettlementTableStat ts : tableStats) {
//            settlement.addTableStat(SettlementTableStat.builder()
//                    .settlement(settlement)
//                    .tableNum(ts.getTableNum())
//                    .useCount(ts.getUseCount())
//                    .salesAmount(ts.getSalesAmount())
//                    .orderCount(ts.getOrderCount())
//                    .avgUsageMinutes(ts.getAvgUsageMinutes())
//                    .build());
//        }
//
//        storeSettlementRepository.save(settlement);
//
//        log.info("── 매장ID: {} 정산 완료 ──", storeId);
//        log.info("  매출: {}원, 주문: {}건, 취소: {}건, 순매출: {}원",
//                dayTotalAmount, orderCount, cancelCount, netSales);
//        log.info("  카드: {}원/{}건, 이체: {}원/{}건, 간편: {}원/{}건, 폰: {}원/{}건",
//                cardAmount, cardCount, transferAmount, transferCount,
//                easyPayAmount, easyPayCount, phoneAmount, phoneCount);
//        log.info("  메뉴: {}종, 카테고리: {}종, 테이블: {}개",
//                menuRanks.size(), categoryRanks.size(), tableStats.size());
//    }
//
//    // ═══════════════════════════════════════════════════════════
//    //  메뉴별 순위 빌드
//    //
//    //  [왜 2중 for문인가?]
//    //  주문 구조가 이렇게 생겼기 때문:
//    //    주문1 → [소주 2개, 삼겹살 1개]
//    //    주문2 → [소주 1개, 맥주 3개]
//    //    주문3 → [삼겹살 2개]
//    //
//    //  메뉴별로 합산하면:
//    //    소주: 2+1 = 3개 / 15,000원
//    //    삼겹살: 1+2 = 3개 / 45,000원
//    //    맥주: 3개 / 15,000원
//    //
//    //  바깥 for = 주문 리스트 순회
//    //  안쪽 for = 해당 주문의 메뉴 리스트 순회
//    //  → Map에 menuId 기준으로 수량/금액 누적
//    //  → 마지막에 건수 순위, 금액 순위 각각 정렬
//    // ═══════════════════════════════════════════════════════════
//
//    private List<SettlementMenuRank> buildMenuRanks(List<Ordering> orders) {
//        // menuId → 집계 데이터 (이름, 카테고리, 수량합, 금액합)
//        Map<Long, MenuAgg> menuMap = new LinkedHashMap<>();
//
//        for (Ordering order : orders) {
//            for (OrderingDetail detail : order.getOrderDetail()) {
//                Long menuId = detail.getMenu().getId();
//                String menuName = detail.getMenu().getMenuName();
//                String categoryName = detail.getMenu().getCategory().getCategoryName();
//                int qty = detail.getOrderingDetailQuantity();
//                int amount = detail.getMenuPrice() * qty;
//
//                // 처음 보는 메뉴면 새로 생성, 있으면 기존 것에 누적
//                menuMap.computeIfAbsent(menuId, k -> new MenuAgg(menuId, menuName, categoryName));
//                MenuAgg agg = menuMap.get(menuId);
//                agg.count += qty;
//                agg.amount += amount;
//            }
//        }
//
//        // 건수 기준 순위 (많이 팔린 순)
//        List<MenuAgg> byCount = new ArrayList<>(menuMap.values());
//        byCount.sort((a, b) -> Integer.compare(b.count, a.count));
//        for (int i = 0; i < byCount.size(); i++) {
//            byCount.get(i).rankByCount = i + 1;
//        }
//
//        // 금액 기준 순위 (매출 높은 순)
//        List<MenuAgg> byAmount = new ArrayList<>(menuMap.values());
//        byAmount.sort((a, b) -> Integer.compare(b.amount, a.amount));
//        for (int i = 0; i < byAmount.size(); i++) {
//            byAmount.get(i).rankByAmount = i + 1;
//        }
//
//        // MenuAgg → SettlementMenuRank 엔티티로 변환
//        return menuMap.values().stream()
//                .map(a -> SettlementMenuRank.builder()
//                        .menuId(a.menuId).menuName(a.menuName).categoryName(a.categoryName)
//                        .salesCount(a.count).salesAmount(a.amount)
//                        .rankByCount(a.rankByCount).rankByAmount(a.rankByAmount)
//                        .build())
//                .toList();
//    }
//
//    // ═══════════════════════════════════════════════════════════
//    //  카테고리별 순위 빌드
//    //
//    //  메뉴별과 같은 원리.
//    //  다만 menuId 대신 categoryName으로 묶어서 합산.
//    //  예: "안주" 카테고리 = 삼겹살(45,000) + 감자튀김(12,000) = 57,000원
//    // ═══════════════════════════════════════════════════════════
//
//    private List<SettlementCategoryRank> buildCategoryRanks(List<Ordering> orders) {
//        // 카테고리명 → [수량합, 금액합]
//        Map<String, long[]> catMap = new LinkedHashMap<>();
//        // 카테고리명 → 카테고리ID (스냅샷 저장용)
//        Map<String, Long> catIdMap = new HashMap<>();
//
//        for (Ordering order : orders) {
//            for (OrderingDetail detail : order.getOrderDetail()) {
//                String catName = detail.getMenu().getCategory().getCategoryName();
//                Long catId = detail.getMenu().getCategory().getId();
//                int qty = detail.getOrderingDetailQuantity();
//                int amount = detail.getMenuPrice() * qty;
//
//                catIdMap.putIfAbsent(catName, catId);
//                catMap.computeIfAbsent(catName, k -> new long[]{0, 0});
//                catMap.get(catName)[0] += qty;     // [0] = 수량
//                catMap.get(catName)[1] += amount;  // [1] = 금액
//            }
//        }
//
//        // 금액 기준 내림차순 정렬 → 순위 매기기
//        List<Map.Entry<String, long[]>> sorted = new ArrayList<>(catMap.entrySet());
//        sorted.sort((a, b) -> Long.compare(b.getValue()[1], a.getValue()[1]));
//
//        List<SettlementCategoryRank> result = new ArrayList<>();
//        for (int i = 0; i < sorted.size(); i++) {
//            Map.Entry<String, long[]> e = sorted.get(i);
//            result.add(SettlementCategoryRank.builder()
//                    .categoryId(catIdMap.get(e.getKey()))
//                    .categoryName(e.getKey())
//                    .salesCount((int) e.getValue()[0])
//                    .salesAmount((int) e.getValue()[1])
//                    .rankByAmount(i + 1)
//                    .build());
//        }
//        return result;
//    }
//
//    // ═══════════════════════════════════════════════════════════
//    //  테이블별 통계 빌드
//    //  [왜 for문 + Map을 쓰는가?]
//    //  두 개의 다른 쿼리 결과를 합쳐야 하기 때문:
//    //    쿼리1 (sumSalesByTable): 테이블별 매출/주문수
//    //      → [1번테이블, 50000원, 3건], [2번테이블, 30000원, 2건]
//    //    쿼리2 (countGroupsByTable): 테이블별 이용횟수
//    //      → [1번테이블, 2회], [2번테이블, 1회]
//    //
//    //  이걸 합치면:
//    //    1번테이블: 매출 50000, 주문 3건, 이용 2회
//    //    2번테이블: 매출 30000, 주문 2건, 이용 1회
//    //
//    //  쿼리2 결과를 먼저 Map에 담아두고,
//    //  쿼리1 결과를 돌면서 같은 테이블번호의 이용횟수를 Map에서 꺼냄
//    // ═══════════════════════════════════════════════════════════
//
//    private List<SettlementTableStat> buildTableStats(Long storeId, LocalDateTime startAt, LocalDateTime endAt) {
//        List<SettlementTableStat> result = new ArrayList<>();
//
//        try {
//            // 쿼리1: 테이블별 [테이블번호, 매출합, 주문수]
//            List<Object[]> tableSalesRaw = orderingRepository.sumSalesByTable(storeId, startAt, endAt);
//
//            // 쿼리2: 테이블별 [테이블번호, 이용횟수(distinct groupId)]
//            List<Object[]> tableGroupRaw = orderingRepository.countGroupsByTable(storeId, startAt, endAt);
//
//            // 쿼리2 결과를 Map에 저장 → 테이블번호로 빠르게 조회하기 위해
//            Map<Integer, Integer> groupCountMap = new HashMap<>();
//            for (Object[] row : tableGroupRaw) {
//                groupCountMap.put(
//                        ((Number) row[0]).intValue(),  // 테이블번호
//                        ((Number) row[1]).intValue()   // 이용횟수
//                );
//            }
//
//            // 쿼리1 결과를 돌면서 쿼리2의 이용횟수와 합침
//            for (Object[] row : tableSalesRaw) {
//                int tableNum = ((Number) row[0]).intValue();
//                int salesAmount = ((Number) row[1]).intValue();
//                int orderCnt = ((Number) row[2]).intValue();
//                // Map에서 해당 테이블의 이용횟수를 꺼냄 (없으면 0)
//                int useCount = groupCountMap.getOrDefault(tableNum, 0);
//
//                result.add(SettlementTableStat.builder()
//                        .tableNum(tableNum)
//                        .useCount(useCount)
//                        .salesAmount(salesAmount)
//                        .orderCount(orderCnt)
//                        .avgUsageMinutes(useCount > 0 ? 60 : 0)  // 기본 추정값
//                        .build());
//            }
//        } catch (Exception e) {
//            log.warn("테이블 통계 실패(매장ID: {}): {}", storeId, e.getMessage());
//        }
//
//        return result;
//    }
//
//    // ═══════════════════════════════════════════════════════════
//    //  내부 헬퍼 클래스
//    //  - buildMenuRanks()에서 메뉴별 데이터를 임시로 담아두는 용도
//    //  - Map<Long, MenuAgg>로 menuId별 수량/금액 누적 후
//    //    마지막에 SettlementMenuRank 엔티티로 변환
//    // ═══════════════════════════════════════════════════════════
//
//    private static class MenuAgg {
//        Long menuId;
//        String menuName;
//        String categoryName;
//        int count = 0;        // 판매 수량 합계
//        int amount = 0;       // 판매 금액 합계
//        int rankByCount = 0;  // 건수 순위 (나중에 정렬 후 세팅)
//        int rankByAmount = 0; // 금액 순위 (나중에 정렬 후 세팅)
//
//        MenuAgg(Long menuId, String menuName, String categoryName) {
//            this.menuId = menuId;
//            this.menuName = menuName;
//            this.categoryName = categoryName;
//        }
//    }
//}

package com.beyond.pochaon.owner.service;

import com.beyond.pochaon.ordering.domain.Ordering;
import com.beyond.pochaon.ordering.domain.OrderingDetail;
import com.beyond.pochaon.ordering.repository.OrderingRepository;
import com.beyond.pochaon.payment.repository.PaymentRepository;
import com.beyond.pochaon.store.domain.*;
import com.beyond.pochaon.store.repository.StoreRepository;
import com.beyond.pochaon.store.repository.StoreSettlementRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Component
public class OwnerScheduler {

    private final StoreRepository storeRepository;
    private final OrderingRepository orderingRepository;
    private final StoreSettlementRepository storeSettlementRepository;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final PaymentRepository paymentRepository;
    @Autowired
    @Lazy
    private OwnerScheduler self;
    @Autowired
    public OwnerScheduler(StoreRepository storeRepository,
                          OrderingRepository orderingRepository,
                          StoreSettlementRepository storeSettlementRepository,
                          @Qualifier("taskSchedular") ThreadPoolTaskScheduler taskScheduler,
                          PaymentRepository paymentRepository) {
        this.storeRepository = storeRepository;
        this.orderingRepository = orderingRepository;
        this.storeSettlementRepository = storeSettlementRepository;
        this.taskScheduler = taskScheduler;
        this.paymentRepository = paymentRepository;
    }

    // ═══════════════════════════════════════════════════════════
    //  ★ 테스트용: 5분마다 모든 매장의 전체 기간 정산
    //  - 각 매장의 가장 오래된 주문 ~ 오늘까지 날짜별 정산
    //  - 주문 없는 날은 자동 스킵
    //  - 이미 있는 날짜는 upsert (최신 데이터로 갱신)
    //
    //  ★ 운영 시: 아래 주석 처리된 원본 스케줄러로 복원하세요
    // ═══════════════════════════════════════════════════════════

    @Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(name = "scheduleSettlement", lockAtLeastFor = "1m", lockAtMostFor = "PT30M")
    public void storeScheduler() {
        log.info("==== 전체 정산 스케줄러 시작 (5분 주기) ====");
        List<Store> stores = storeRepository.findAll();
        LocalDate today = LocalDate.now();

        for (Store store : stores) {
            try {
                // 해당 매장의 가장 오래된 주문 날짜 조회
                LocalDateTime earliest = orderingRepository.findEarliestOrderDate(store.getId());
                if (earliest == null) {
                    log.debug("매장ID {}: 주문 데이터 없음 → 스킵", store.getId());
                    continue;
                }

                LocalDate startDate = earliest.toLocalDate();
                log.info("매장ID {}: {} ~ {} 정산 시작", store.getId(), startDate, today);

                int count = 0;
                LocalDate current = startDate;
                while (!current.isAfter(today)) {
                    try {
                        self.settlementForDate(store, current);
                        count++;
                    } catch (Exception e) {
                        log.warn("정산 실패: 매장ID={}, 날짜={}, 에러={}", store.getId(), current, e.getMessage());
                    }
                    current = current.plusDays(1);
                }

                log.info("매장ID {}: {}일치 정산 완료", store.getId(), count);
            } catch (Exception e) {
                log.error("매장ID {} 정산 중 오류: {}", store.getId(), e.getMessage());
            }
        }
        log.info("==== 전체 정산 스케줄러 완료 ====");
    }

    /*
    // ═══════════════════════════════════════════════════════════
    //  [원본] 매일 오후 3시 → 마감시간에 예약 실행
    //  운영 시 이걸로 복원하세요
    // ═══════════════════════════════════════════════════════════

    @Scheduled(cron = "0 0 15 * * *")
    @SchedulerLock(name = "scheduleSettlement", lockAtLeastFor = "1m", lockAtMostFor = "PT30M")
    public void storeScheduler() {
        log.info("==== 정산 예약 스케줄러 시작 ====");
        List<Store> stores = storeRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Store store : stores) {
            LocalTime closedAt = store.getStoreCloseAt();
            if (closedAt == null) {
                log.warn("매장ID {} 마감시간이 null → 스케줄링 스킵", store.getId());
                continue;
            }

            LocalDateTime targetDateTime = LocalDateTime.of(LocalDate.now(), closedAt);
            if (targetDateTime.isBefore(now)) {
                targetDateTime = targetDateTime.plusDays(1);
            }

            log.info("매장ID: {}, 예약시간: {}", store.getId(), targetDateTime);
            taskScheduler.schedule(() -> settlement(store.getId()),
                    targetDateTime.atZone(ZoneId.systemDefault()).toInstant());
        }
        log.info("==== 정산 예약 스케줄러 완료 ====");
    }
    */

    // ═══════════════════════════════════════════════════════════
    //  ★ 특정 날짜 1일 정산 (백필 핵심)
    // ═══════════════════════════════════════════════════════════

    @Transactional
    public void settlementForDate(Store store, LocalDate targetDate) {
        Long storeId = store.getId();

        LocalDateTime startAt = targetDate.atStartOfDay();
        LocalDateTime endAt = targetDate.plusDays(1).atStartOfDay();

        // 1단계: 기본 매출 집계
        int dayTotalAmount = 0;
        Integer rawRevenue = orderingRepository.sumTotalRevenue(storeId, startAt, endAt);
        if (rawRevenue != null && rawRevenue > 0) {
            dayTotalAmount = rawRevenue;
        }

        int orderCount = orderingRepository.countCompletedOrders(storeId, startAt, endAt);
        if (orderCount == 0 && dayTotalAmount == 0) return; // 주문 없으면 스킵

        int averageOrderAmount = orderCount > 0 ? dayTotalAmount / orderCount : 0;
        int cancelCount = orderingRepository.countCancelledOrders(storeId, startAt, endAt);
        int tableUseCount = orderingRepository.countDistinctGroupIds(storeId, startAt, endAt);
        int refundAmount = 0;
        int netSales = dayTotalAmount - refundAmount;

        // 2단계: 결제수단별
        int cardAmount = 0, cardCount = 0, transferAmount = 0, transferCount = 0;
        int easyPayAmount = 0, easyPayCount = 0, phoneAmount = 0, phoneCount = 0;

        try {
            for (Object[] row : paymentRepository.sumByPaymentMethod(storeId, startAt, endAt)) {
                String method = (String) row[0];
                int amount = ((Number) row[1]).intValue();
                int count = ((Number) row[2]).intValue();
                if (method == null) continue;
                switch (method) {
                    case "카드" -> { cardAmount = amount; cardCount = count; }
                    case "계좌이체" -> { transferAmount = amount; transferCount = count; }
                    case "간편결제" -> { easyPayAmount = amount; easyPayCount = count; }
                    case "휴대폰" -> { phoneAmount = amount; phoneCount = count; }
                }
            }
        } catch (Exception e) {
            log.warn("결제수단 집계 실패: 매장={}, 날짜={}", storeId, targetDate);
        }

        // 3단계: 메뉴/카테고리/테이블
        List<Ordering> completedOrders = orderingRepository.findCompletedOrdersWithDetails(storeId, startAt, endAt);
        List<SettlementMenuRank> menuRanks = buildMenuRanks(completedOrders);
        List<SettlementCategoryRank> categoryRanks = buildCategoryRanks(completedOrders);
        List<SettlementTableStat> tableStats = buildTableStats(storeId, startAt, endAt);

        // 5단계: upsert
        Optional<StoreSettlement> existingOpt = storeSettlementRepository.findByStoreAndSettlementDate(store, targetDate);
        StoreSettlement settlement;

        if (existingOpt.isPresent()) {
            settlement = existingOpt.get();
            settlement.update(dayTotalAmount, orderCount, averageOrderAmount,
                    cancelCount, refundAmount, netSales,
                    cardAmount, cardCount, transferAmount, transferCount,
                    easyPayAmount, easyPayCount, phoneAmount, phoneCount, tableUseCount);
            settlement.clearChildren();
            storeSettlementRepository.saveAndFlush(settlement);
        } else {
            settlement = StoreSettlement.builder()
                    .store(store).settlementDate(targetDate)
                    .dayTotalAmount(dayTotalAmount).orderCount(orderCount).averageOrderAmount(averageOrderAmount)
                    .cancelCount(cancelCount).refundAmount(refundAmount).netSales(netSales)
                    .cardAmount(cardAmount).cardCount(cardCount)
                    .transferAmount(transferAmount).transferCount(transferCount)
                    .easyPayAmount(easyPayAmount).easyPayCount(easyPayCount)
                    .phoneAmount(phoneAmount).phoneCount(phoneCount)
                    .tableUseCount(tableUseCount)
                    .build();
        }

        for (SettlementMenuRank mr : menuRanks)
            settlement.addMenuRank(SettlementMenuRank.builder().settlement(settlement)
                    .menuId(mr.getMenuId()).menuName(mr.getMenuName()).categoryName(mr.getCategoryName())
                    .salesCount(mr.getSalesCount()).salesAmount(mr.getSalesAmount())
                    .rankByCount(mr.getRankByCount()).rankByAmount(mr.getRankByAmount()).build());
        for (SettlementCategoryRank cr : categoryRanks)
            settlement.addCategoryRank(SettlementCategoryRank.builder().settlement(settlement)
                    .categoryId(cr.getCategoryId()).categoryName(cr.getCategoryName())
                    .salesCount(cr.getSalesCount()).salesAmount(cr.getSalesAmount())
                    .rankByAmount(cr.getRankByAmount()).build());
        for (SettlementTableStat ts : tableStats)
            settlement.addTableStat(SettlementTableStat.builder().settlement(settlement)
                    .tableNum(ts.getTableNum()).useCount(ts.getUseCount())
                    .salesAmount(ts.getSalesAmount()).orderCount(ts.getOrderCount())
                    .avgUsageMinutes(ts.getAvgUsageMinutes()).build());

        storeSettlementRepository.save(settlement);
    }

    // ═══════════════════════════════════════════════════════════
    //  기존 오늘 정산 (그대로 유지)
    // ═══════════════════════════════════════════════════════════

    @Transactional
    public void settlement(Long storeId) {
        log.info("── 매장ID: {} 정산 시작 ──", storeId);

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("없는 매장입니다."));

        LocalTime openTime = store.getStoreOpenAt();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentClosedAt = now;
        LocalDateTime currentOpenedAt = LocalDateTime.of(LocalDate.now(), openTime);
        if (currentOpenedAt.isAfter(now)) currentOpenedAt = currentOpenedAt.minusDays(1);

        LocalDate today = LocalDate.now();

        int dayTotalAmount = 0;
        Integer rawRevenue = orderingRepository.sumTotalRevenue(storeId, currentOpenedAt, currentClosedAt);
        if (rawRevenue != null && rawRevenue > 0) dayTotalAmount = rawRevenue;

        int orderCount = orderingRepository.countCompletedOrders(storeId, currentOpenedAt, currentClosedAt);
        int averageOrderAmount = orderCount > 0 ? dayTotalAmount / orderCount : 0;
        int cancelCount = orderingRepository.countCancelledOrders(storeId, currentOpenedAt, currentClosedAt);
        int tableUseCount = orderingRepository.countDistinctGroupIds(storeId, currentOpenedAt, currentClosedAt);
        int refundAmount = 0;
        int netSales = dayTotalAmount - refundAmount;

        int cardAmount = 0, cardCount = 0, transferAmount = 0, transferCount = 0;
        int easyPayAmount = 0, easyPayCount = 0, phoneAmount = 0, phoneCount = 0;
        try {
            for (Object[] row : paymentRepository.sumByPaymentMethod(storeId, currentOpenedAt, currentClosedAt)) {
                String method = (String) row[0];
                int amount = ((Number) row[1]).intValue();
                int count = ((Number) row[2]).intValue();
                if (method == null) continue;
                switch (method) {
                    case "카드" -> { cardAmount = amount; cardCount = count; }
                    case "계좌이체" -> { transferAmount = amount; transferCount = count; }
                    case "간편결제" -> { easyPayAmount = amount; easyPayCount = count; }
                    case "휴대폰" -> { phoneAmount = amount; phoneCount = count; }
                }
            }
        } catch (Exception e) {
            log.warn("결제수단 집계 실패(매장ID: {}): {}", storeId, e.getMessage());
        }

        List<Ordering> completedOrders = orderingRepository.findCompletedOrdersWithDetails(storeId, currentOpenedAt, currentClosedAt);
        List<SettlementMenuRank> menuRanks = buildMenuRanks(completedOrders);
        List<SettlementCategoryRank> categoryRanks = buildCategoryRanks(completedOrders);
        List<SettlementTableStat> tableStats = buildTableStats(storeId, currentOpenedAt, currentClosedAt);

        Optional<StoreSettlement> existingOpt = storeSettlementRepository.findByStoreAndSettlementDate(store, today);
        StoreSettlement settlement;

        if (existingOpt.isPresent()) {
            settlement = existingOpt.get();
            settlement.update(dayTotalAmount, orderCount, averageOrderAmount,
                    cancelCount, refundAmount, netSales,
                    cardAmount, cardCount, transferAmount, transferCount,
                    easyPayAmount, easyPayCount, phoneAmount, phoneCount, tableUseCount);
            settlement.clearChildren();
            storeSettlementRepository.saveAndFlush(settlement);
        } else {
            settlement = StoreSettlement.builder()
                    .store(store).settlementDate(today)
                    .dayTotalAmount(dayTotalAmount).orderCount(orderCount).averageOrderAmount(averageOrderAmount)
                    .cancelCount(cancelCount).refundAmount(refundAmount).netSales(netSales)
                    .cardAmount(cardAmount).cardCount(cardCount)
                    .transferAmount(transferAmount).transferCount(transferCount)
                    .easyPayAmount(easyPayAmount).easyPayCount(easyPayCount)
                    .phoneAmount(phoneAmount).phoneCount(phoneCount)
                    .tableUseCount(tableUseCount).build();
        }

        for (SettlementMenuRank mr : menuRanks)
            settlement.addMenuRank(SettlementMenuRank.builder().settlement(settlement)
                    .menuId(mr.getMenuId()).menuName(mr.getMenuName()).categoryName(mr.getCategoryName())
                    .salesCount(mr.getSalesCount()).salesAmount(mr.getSalesAmount())
                    .rankByCount(mr.getRankByCount()).rankByAmount(mr.getRankByAmount()).build());
        for (SettlementCategoryRank cr : categoryRanks)
            settlement.addCategoryRank(SettlementCategoryRank.builder().settlement(settlement)
                    .categoryId(cr.getCategoryId()).categoryName(cr.getCategoryName())
                    .salesCount(cr.getSalesCount()).salesAmount(cr.getSalesAmount())
                    .rankByAmount(cr.getRankByAmount()).build());
        for (SettlementTableStat ts : tableStats)
            settlement.addTableStat(SettlementTableStat.builder().settlement(settlement)
                    .tableNum(ts.getTableNum()).useCount(ts.getUseCount())
                    .salesAmount(ts.getSalesAmount()).orderCount(ts.getOrderCount())
                    .avgUsageMinutes(ts.getAvgUsageMinutes()).build());

        storeSettlementRepository.save(settlement);
        log.info("── 매장ID: {} 정산 완료 ── 매출: {}원, 주문: {}건", storeId, dayTotalAmount, orderCount);
    }

    // ═══════════════════════════════════════════════════════════
    //  빌드 헬퍼들
    // ═══════════════════════════════════════════════════════════

    private List<SettlementMenuRank> buildMenuRanks(List<Ordering> orders) {
        Map<Long, MenuAgg> menuMap = new LinkedHashMap<>();
        for (Ordering order : orders) {
            for (OrderingDetail detail : order.getOrderDetail()) {
                Long menuId = detail.getMenu().getId();
                menuMap.computeIfAbsent(menuId, k -> new MenuAgg(menuId,
                        detail.getMenu().getMenuName(), detail.getMenu().getCategory().getCategoryName()));
                MenuAgg agg = menuMap.get(menuId);
                int qty = detail.getOrderingDetailQuantity();
                agg.count += qty;
                agg.amount += detail.getMenuPrice() * qty;
            }
        }
        List<MenuAgg> byCount = new ArrayList<>(menuMap.values());
        byCount.sort((a, b) -> Integer.compare(b.count, a.count));
        for (int i = 0; i < byCount.size(); i++) byCount.get(i).rankByCount = i + 1;
        List<MenuAgg> byAmount = new ArrayList<>(menuMap.values());
        byAmount.sort((a, b) -> Integer.compare(b.amount, a.amount));
        for (int i = 0; i < byAmount.size(); i++) byAmount.get(i).rankByAmount = i + 1;

        return menuMap.values().stream()
                .map(a -> SettlementMenuRank.builder()
                        .menuId(a.menuId).menuName(a.menuName).categoryName(a.categoryName)
                        .salesCount(a.count).salesAmount(a.amount)
                        .rankByCount(a.rankByCount).rankByAmount(a.rankByAmount).build())
                .toList();
    }

    private List<SettlementCategoryRank> buildCategoryRanks(List<Ordering> orders) {
        Map<String, long[]> catMap = new LinkedHashMap<>();
        Map<String, Long> catIdMap = new HashMap<>();
        for (Ordering order : orders) {
            for (OrderingDetail detail : order.getOrderDetail()) {
                String catName = detail.getMenu().getCategory().getCategoryName();
                catIdMap.putIfAbsent(catName, detail.getMenu().getCategory().getId());
                catMap.computeIfAbsent(catName, k -> new long[]{0, 0});
                int qty = detail.getOrderingDetailQuantity();
                catMap.get(catName)[0] += qty;
                catMap.get(catName)[1] += detail.getMenuPrice() * qty;
            }
        }
        List<Map.Entry<String, long[]>> sorted = new ArrayList<>(catMap.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue()[1], a.getValue()[1]));
        List<SettlementCategoryRank> result = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, long[]> e = sorted.get(i);
            result.add(SettlementCategoryRank.builder()
                    .categoryId(catIdMap.get(e.getKey())).categoryName(e.getKey())
                    .salesCount((int) e.getValue()[0]).salesAmount((int) e.getValue()[1])
                    .rankByAmount(i + 1).build());
        }
        return result;
    }

    private List<SettlementTableStat> buildTableStats(Long storeId, LocalDateTime startAt, LocalDateTime endAt) {
        List<SettlementTableStat> result = new ArrayList<>();
        try {
            List<Object[]> tableSalesRaw = orderingRepository.sumSalesByTable(storeId, startAt, endAt);
            List<Object[]> tableGroupRaw = orderingRepository.countGroupsByTable(storeId, startAt, endAt);
            Map<Integer, Integer> groupCountMap = new HashMap<>();
            for (Object[] row : tableGroupRaw) groupCountMap.put(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
            for (Object[] row : tableSalesRaw) {
                int tableNum = ((Number) row[0]).intValue();
                result.add(SettlementTableStat.builder()
                        .tableNum(tableNum).useCount(groupCountMap.getOrDefault(tableNum, 0))
                        .salesAmount(((Number) row[1]).intValue()).orderCount(((Number) row[2]).intValue())
                        .avgUsageMinutes(groupCountMap.containsKey(tableNum) ? 60 : 0).build());
            }
        } catch (Exception e) {
            log.warn("테이블 통계 실패(매장ID: {}): {}", storeId, e.getMessage());
        }
        return result;
    }

    private static class MenuAgg {
        Long menuId; String menuName; String categoryName;
        int count = 0, amount = 0, rankByCount = 0, rankByAmount = 0;
        MenuAgg(Long menuId, String menuName, String categoryName) {
            this.menuId = menuId; this.menuName = menuName; this.categoryName = categoryName;
        }
    }
}
