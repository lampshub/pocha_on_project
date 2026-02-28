package com.beyond.pochaon.store.service;

import com.beyond.pochaon.ordering.domain.Ordering;
import com.beyond.pochaon.ordering.domain.OrderingDetail;
import com.beyond.pochaon.ordering.repository.OrderingRepository;
import com.beyond.pochaon.payment.entity.Payment;
import com.beyond.pochaon.payment.repository.PaymentRepository;
import com.beyond.pochaon.store.domain.SettlementCategoryRank;
import com.beyond.pochaon.store.domain.SettlementMenuRank;
import com.beyond.pochaon.store.domain.SettlementTableStat;
import com.beyond.pochaon.store.domain.StoreSettlement;
import com.beyond.pochaon.store.repository.StoreSettlementRepository;
import com.beyond.pochaon.store.settlementdto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Slf4j
public class StoreSettlementService {

    private final StoreSettlementRepository storeSettlementRepository;
    private final OrderingRepository orderingRepository;
    private final PaymentRepository paymentRepository;

    @Autowired
    public StoreSettlementService(StoreSettlementRepository storeSettlementRepository,
                                  OrderingRepository orderingRepository,
                                  PaymentRepository paymentRepository) {
        this.storeSettlementRepository = storeSettlementRepository;
        this.orderingRepository = orderingRepository;
        this.paymentRepository = paymentRepository;
    }

    // ═══════════════════════════════════════════════════════════
    //  공통 내부 모델 (Weekly/Monthly 중복 제거용)
    // ═══════════════════════════════════════════════════════════

    private record MenuAgg(String menuName, String categoryName, int salesCount, int salesAmount) {
    }

    private record CategoryAgg(String categoryName, int salesCount, int salesAmount) {
    }

    private record TableAgg(int tableNum, int useCount, int salesAmount, int orderCount, int avgUsageMinutes) {
    }

    private record PaymentMethodAgg(int cardAmount, int cardCount, int transferAmount, int transferCount,
                                    int easyPayAmount, int easyPayCount, int phoneAmount, int phoneCount) {
    }

    private record SettlementTotals(int totalAmount, int orderCount, int averageOrderAmount,
                                    int cancelCount, int refundAmount, int netSales,
                                    int tableUseCount, PaymentMethodAgg pma) {
    }

    // ═══════════════════════════════════════════════════════════
    //  일별 정산 (실시간 - Ordering, Payment 원본 테이블 조회)
    // ═══════════════════════════════════════════════════════════

    public DailySettlementResDto getDailySettlement(Long storeId, PeriodReqDto dto) {
        LocalDateTime startAt = dto.getStartAt();
        LocalDateTime endAt = dto.getEndAt();

        // ── 1. 기본 집계 ──

        int rawRevenue = orderingRepository.sumTotalRevenue(storeId, startAt, endAt);
        int totalAmount = Math.max(rawRevenue, 0);

        int orderCount = orderingRepository.countCompletedOrders(storeId, startAt, endAt);
        int cancelCount = orderingRepository.countCancelledOrders(storeId, startAt, endAt);
        int tableUseCount = orderingRepository.countDistinctGroupIds(storeId, startAt, endAt);
        int averageOrderAmount = orderCount > 0 ? totalAmount / orderCount : 0;
        int refundAmount = 0;
        int netSales = totalAmount - refundAmount;

        // ── 2. 결제수단별 ──

        PaymentMethodAgg pma = aggregatePaymentMethods(storeId, startAt, endAt);

        // ── 3. 주문 목록 + 메뉴/카테고리/테이블 집계 ──

        List<Ordering> orders = orderingRepository.findCompletedOrdersWithDetails(storeId, startAt, endAt);

        // ★ [N+1 해결] groupId → 결제수단을 1회 batch 조회
        Set<String> allGroupIds = orders.stream()
                .map(Ordering::getGroupId)
                .filter(Objects::nonNull)
                .map(UUID::toString)
                .collect(Collectors.toSet());
        Map<String, String> groupPaymentMethodMap = batchLoadPaymentMethods(allGroupIds);

        Map<String, int[]> menuMap = new LinkedHashMap<>();
        Map<String, String> menuCategoryMap = new HashMap<>();
        Map<String, int[]> catMap = new LinkedHashMap<>();
        Map<Integer, int[]> tableMap = new LinkedHashMap<>();
        Set<String> tableGroupSet = new HashSet<>();
        List<DailySettlementResDto.OrderItem> orderItems = new ArrayList<>(orders.size());

        for (Ordering order : orders) {
            List<DailySettlementResDto.OrderMenuItem> menuItems = new ArrayList<>();

            for (OrderingDetail detail : order.getOrderDetail()) {
                String menuName = detail.getMenu().getMenuName();
                String categoryName = detail.getMenu().getCategory().getCategoryName();
                int qty = detail.getOrderingDetailQuantity();
                int amount = detail.getMenuPrice() * qty;

                menuMap.computeIfAbsent(menuName, k -> new int[]{0, 0});
                menuMap.get(menuName)[0] += qty;
                menuMap.get(menuName)[1] += amount;
                menuCategoryMap.putIfAbsent(menuName, categoryName);

                catMap.computeIfAbsent(categoryName, k -> new int[]{0, 0});
                catMap.get(categoryName)[0] += qty;
                catMap.get(categoryName)[1] += amount;

                List<String> options = detail.getOrderingDetailOptions() != null
                        ? detail.getOrderingDetailOptions().stream()
                        .map(opt -> opt.getOrderingOptionName()).toList()
                        : List.of();

                menuItems.add(DailySettlementResDto.OrderMenuItem.builder()
                        .menuName(menuName).quantity(qty).price(amount).options(options).build());
            }

            int tableNum = order.getCustomerTable().getTableNum();
            tableMap.computeIfAbsent(tableNum, k -> new int[]{0, 0, 0});
            tableMap.get(tableNum)[1] += order.getTotalPrice();
            tableMap.get(tableNum)[2] += 1;
            if (order.getGroupId() != null) {
                String key = tableNum + ":" + order.getGroupId();
                if (tableGroupSet.add(key)) {
                    tableMap.get(tableNum)[0] += 1;
                }
            }

            // ★ [N+1 해결] Map에서 O(1) 조회
            String paymentMethod = order.getGroupId() != null
                    ? groupPaymentMethodMap.get(order.getGroupId().toString())
                    : null;

            orderItems.add(DailySettlementResDto.OrderItem.builder()
                    .orderingId(order.getId())
                    .tableNum(tableNum)
                    .totalPrice(order.getTotalPrice())
                    .orderStatus(order.getOrderStatus().name())
                    .paymentMethod(paymentMethod)
                    .orderedAt(order.getCreateTimeAt())
                    .menus(menuItems).build());
        }

        // ── 4~6. 순위/통계 ──

        List<DailySettlementResDto.MenuRankItem> menuByCount = buildMenuRank(menuMap, menuCategoryMap, true);
        List<DailySettlementResDto.MenuRankItem> menuByAmount = buildMenuRank(menuMap, menuCategoryMap, false);
        List<DailySettlementResDto.CategoryRankItem> categoryRank = buildCategoryRank(catMap);

        List<DailySettlementResDto.TableStatItem> tableStats = tableMap.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue()[1], a.getValue()[1]))
                .map(e -> DailySettlementResDto.TableStatItem.builder()
                        .tableNum(e.getKey()).useCount(e.getValue()[0])
                        .salesAmount(e.getValue()[1]).orderCount(e.getValue()[2]).build())
                .collect(Collectors.toList());

        return DailySettlementResDto.builder()
                .date(LocalDate.of(dto.getYear(), dto.getMonth(), dto.getDay()))
                .totalAmount(totalAmount).orderCount(orderCount).averageOrderAmount(averageOrderAmount)
                .cancelCount(cancelCount).refundAmount(refundAmount).netSales(netSales)
                .tableUseCount(tableUseCount)
                .cardAmount(pma.cardAmount).cardCount(pma.cardCount)
                .transferAmount(pma.transferAmount).transferCount(pma.transferCount)
                .easyPayAmount(pma.easyPayAmount).easyPayCount(pma.easyPayCount)
                .phoneAmount(pma.phoneAmount).phoneCount(pma.phoneCount)
                .menuRankByCount(menuByCount).menuRankByAmount(menuByAmount)
                .categoryRank(categoryRank).tableStats(tableStats)
                .orders(orderItems)
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  주간 정산 (StoreSettlement 합산, 월~일 기준)
    // ═══════════════════════════════════════════════════════════

    public WeeklySettlementResDto getWeeklySettlement(Long storeId, PeriodReqDto dto) {
        LocalDate weekStart = dto.getWeekStart();
        LocalDate weekEnd = dto.getWeekEnd();

        List<StoreSettlement> settlements = storeSettlementRepository
                .findByStoreIdAndSettlementDateRange(storeId, weekStart, weekEnd.plusDays(1));

        SettlementTotals totals = sumSettlements(settlements);

        // ── 일별 breakdown ──
        Map<LocalDate, StoreSettlement> dateMap = settlements.stream()
                .collect(Collectors.toMap(StoreSettlement::getSettlementDate, s -> s, (a, b) -> a));

        List<WeeklySettlementResDto.DailyItem> dailyBreakdown = new ArrayList<>(7);
        for (LocalDate d = weekStart; !d.isAfter(weekEnd); d = d.plusDays(1)) {
            StoreSettlement ss = dateMap.get(d);
            dailyBreakdown.add(WeeklySettlementResDto.DailyItem.builder()
                    .date(d)
                    .dayOfWeek(d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN))
                    .totalAmount(ss != null ? ss.getDayTotalAmount() : 0)
                    .orderCount(ss != null ? ss.getOrderCount() : 0)
                    .build());
        }

        // ── 자식 엔티티 (공통 집계 → DTO 변환) ──
        List<MenuAgg> menuAggs = aggregateMenuRanks(settlements);
        List<CategoryAgg> catAggs = aggregateCategoryRanks(settlements);
        List<TableAgg> tableAggs = aggregateTableStats(settlements);

        return WeeklySettlementResDto.builder()
                .weekStart(weekStart).weekEnd(weekEnd)
                .totalAmount(totals.totalAmount).orderCount(totals.orderCount)
                .averageOrderAmount(totals.averageOrderAmount)
                .cancelCount(totals.cancelCount).refundAmount(totals.refundAmount).netSales(totals.netSales)
                .tableUseCount(totals.tableUseCount)
                .cardAmount(totals.pma.cardAmount).cardCount(totals.pma.cardCount)
                .transferAmount(totals.pma.transferAmount).transferCount(totals.pma.transferCount)
                .easyPayAmount(totals.pma.easyPayAmount).easyPayCount(totals.pma.easyPayCount)
                .phoneAmount(totals.pma.phoneAmount).phoneCount(totals.pma.phoneCount)
                .dailyBreakdown(dailyBreakdown)
                .menuRankByCount(toWeeklyMenuRank(menuAggs, true))
                .menuRankByAmount(toWeeklyMenuRank(menuAggs, false))
                .categoryRank(toWeeklyCategoryRank(catAggs))
                .tableStats(toWeeklyTableStats(tableAggs))
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  월별 정산 (StoreSettlement 합산)
    // ═══════════════════════════════════════════════════════════

    public MonthlySettlementResDto getMonthlySettlement(Long storeId, PeriodReqDto dto) {
        LocalDate monthStart = dto.getMonthStart();
        LocalDate monthEnd = dto.getMonthEnd();

        List<StoreSettlement> settlements = storeSettlementRepository
                .findByStoreIdAndSettlementDateRange(storeId, monthStart, monthEnd);

        SettlementTotals totals = sumSettlements(settlements);

        Map<Integer, Integer> dailySales = new HashMap<>();
        for (StoreSettlement ss : settlements) {
            dailySales.put(ss.getSettlementDate().getDayOfMonth(), ss.getDayTotalAmount());
        }

        int daysWithSales = Math.max(settlements.size(), 1);
        int dailyAverageSales = totals.totalAmount / daysWithSales;

        List<MonthlySettlementResDto.WeeklyItem> weeklyBreakdown = buildWeeklyBreakdown(settlements, dto.getYear(), dto.getMonth());
        List<MenuAgg> menuAggs = aggregateMenuRanks(settlements);
        List<CategoryAgg> catAggs = aggregateCategoryRanks(settlements);
        List<TableAgg> tableAggs = aggregateTableStats(settlements);

        return MonthlySettlementResDto.builder()
                .year(dto.getYear()).month(dto.getMonth())
                .totalAmount(totals.totalAmount).orderCount(totals.orderCount)
                .averageOrderAmount(totals.averageOrderAmount).dailyAverageSales(dailyAverageSales)
                .cancelCount(totals.cancelCount).refundAmount(totals.refundAmount).netSales(totals.netSales)
                .tableUseCount(totals.tableUseCount)
                .cardAmount(totals.pma.cardAmount).cardCount(totals.pma.cardCount)
                .transferAmount(totals.pma.transferAmount).transferCount(totals.pma.transferCount)
                .easyPayAmount(totals.pma.easyPayAmount).easyPayCount(totals.pma.easyPayCount)
                .phoneAmount(totals.pma.phoneAmount).phoneCount(totals.pma.phoneCount)
                .dailySales(dailySales).weeklyBreakdown(weeklyBreakdown)
                .menuRankByCount(toMonthlyMenuRank(menuAggs, true))
                .menuRankByAmount(toMonthlyMenuRank(menuAggs, false))
                .categoryRank(toMonthlyCategoryRank(catAggs))
                .tableStats(toMonthlyTableStats(tableAggs))
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  메뉴 분석 (실시간)
    // ═══════════════════════════════════════════════════════════

    public MenuAnalysisResDto getMenuAnalysis(Long storeId, PeriodReqDto dto) {
        LocalDateTime startAt = dto.getStartAt();
        LocalDateTime endAt = dto.getEndAt();

        List<Ordering> orders = orderingRepository.findCompletedOrdersWithDetails(storeId, startAt, endAt);

        Map<String, Integer> categoryMap = new LinkedHashMap<>();
        Map<String, int[]> menuMap = new LinkedHashMap<>();
        Map<UUID, Set<String>> groupMenus = new HashMap<>();

        for (Ordering order : orders) {
            Set<String> menuNames = new HashSet<>();
            for (OrderingDetail detail : order.getOrderDetail()) {
                String menuName = detail.getMenu().getMenuName();
                String categoryName = detail.getMenu().getCategory().getCategoryName();
                int qty = detail.getOrderingDetailQuantity();
                int amount = detail.getMenuPrice() * qty;

                categoryMap.merge(categoryName, amount, Integer::sum);
                menuMap.computeIfAbsent(menuName, k -> new int[]{0, 0});
                menuMap.get(menuName)[0] += qty;
                menuMap.get(menuName)[1] += amount;
                menuNames.add(menuName);
            }
            if (order.getGroupId() != null) {
                groupMenus.computeIfAbsent(order.getGroupId(), k -> new HashSet<>()).addAll(menuNames);
            }
        }

        int maxCatAmount = categoryMap.values().stream().mapToInt(v -> v).max().orElse(1);
        List<MenuAnalysisResDto.CategorySales> categorySales = categoryMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> MenuAnalysisResDto.CategorySales.builder()
                        .name(e.getKey()).amount(e.getValue())
                        .rate(calcRate(e.getValue(), maxCatAmount))
                        .build())
                .collect(Collectors.toList());

        int totalMenuAmount = menuMap.values().stream().mapToInt(v -> v[1]).sum();
        List<MenuAnalysisResDto.MenuRank> menuRanking = menuMap.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue()[1], a.getValue()[1]))
                .map(e -> MenuAnalysisResDto.MenuRank.builder()
                        .name(e.getKey()).qty(e.getValue()[0]).amount(e.getValue()[1])
                        .rate(calcRate(e.getValue()[1], totalMenuAmount))
                        .build())
                .collect(Collectors.toList());

        List<MenuAnalysisResDto.MenuCombo> combos = buildCombos(groupMenus);

        return MenuAnalysisResDto.builder()
                .categorySales(categorySales).menuRanking(menuRanking).combos(combos).build();
    }

    // ═══════════════════════════════════════════════════════════
    //  매출 분석
    // ═══════════════════════════════════════════════════════════

    public SalesAnalysisResDto getSalesAnalysis(Long storeId, PeriodReqDto dto) {
        String period = dto.getPeriod() != null ? dto.getPeriod() : "weekly";

        List<SalesAnalysisResDto.SalesBar> weeklyBars = null;
        List<SalesAnalysisResDto.SalesBar> monthlyBars = null;
        List<SalesAnalysisResDto.HourlySales> hourlySales = null;
        List<SalesAnalysisResDto.SalesBar> dayOfWeekSales = null;

        switch (period) {
            case "weekly": {
                // ★ 프론트가 보낸 day = 해당 주 월요일 날짜
                LocalDate weekStart = dto.getWeekStart();
                LocalDate weekEnd = dto.getWeekEnd().plusDays(1); // 일요일 다음날 (exclusive)
                List<StoreSettlement> settlements = storeSettlementRepository
                        .findByStoreIdAndSettlementDateRange(storeId, weekStart, weekEnd);

                // 해당 주의 일별 매출 7개 (월~일)
                weeklyBars = new ArrayList<>();
                String[] dayNames = {"월", "화", "수", "목", "금", "토", "일"};
                Map<LocalDate, Integer> dateMap = settlements.stream()
                        .collect(Collectors.toMap(
                                StoreSettlement::getSettlementDate,
                                StoreSettlement::getDayTotalAmount,
                                (a, b) -> a
                        ));
                for (int i = 0; i < 7; i++) {
                    LocalDate d = weekStart.plusDays(i);
                    int sales = dateMap.getOrDefault(d, 0);
                    weeklyBars.add(SalesAnalysisResDto.SalesBar.builder()
                            .label(dayNames[i] + " " + d.getMonthValue() + "/" + d.getDayOfMonth())
                            .value(sales)
                            .build());
                }
                break;
            }
            case "monthly": {
                monthlyBars = buildMonthlyBarsOptimized(storeId, dto.getYear());
                break;
            }
            case "hourly": {
                LocalDateTime startAt = dto.getStartAt();
                LocalDateTime endAt = dto.getEndAt();
                List<Object[]> hourlyRaw = orderingRepository.sumSalesByHour(storeId, startAt, endAt);
                hourlySales = buildHourlySales(hourlyRaw);
                break;
            }
            case "dow": {
                LocalDateTime startAt = dto.getStartAt();
                LocalDateTime endAt = dto.getEndAt();
                List<Object[]> dowRaw = orderingRepository.sumSalesByDayOfWeek(storeId, startAt, endAt);
                dayOfWeekSales = buildDayOfWeekSales(dowRaw);
                break;
            }
        }

        // 매출 비교는 항상 현재 월 기준
        YearMonth ym = YearMonth.of(dto.getYear(), dto.getMonth());
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.plusMonths(1).atDay(1);
        List<StoreSettlement> monthSettlements = storeSettlementRepository
                .findByStoreIdAndSettlementDateRange(storeId, monthStart, monthEnd);
        SalesAnalysisResDto.SalesCompare compare = buildSalesCompareOptimized(
                storeId, dto.getYear(), dto.getMonth(), monthSettlements);

        return SalesAnalysisResDto.builder()
                .weeklyBars(weeklyBars)
                .monthlyBars(monthlyBars)
                .hourlySales(hourlySales)
                .dayOfWeekSales(dayOfWeekSales)
                .compare(compare)
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  결제 분석
    // ═══════════════════════════════════════════════════════════
    public PaymentAnalysisResDto getPaymentAnalysis(Long storeId, PeriodReqDto dto) {
        // ★ 수정: dto.day 반영 (day=0이면 월 전체, day>0이면 해당 일자)
        LocalDateTime startAt = dto.getStartAt();
        LocalDateTime endAt = dto.getEndAt();
        YearMonth ym = YearMonth.of(dto.getYear(), dto.getMonth());

        List<Object[]> methodRaw = paymentRepository.sumByPaymentMethod(storeId, startAt, endAt);
        int totalAmount = 0, totalCount = 0;
        int cardAmount = 0, easyPayAmount = 0, transferAmount = 0, phoneAmount = 0;
        int cardCount = 0, easyPayCount = 0, transferCount = 0, phoneCount = 0;

        for (Object[] row : methodRaw) {
            String method = (String) row[0];
            int amount = ((Number) row[1]).intValue();
            int count = ((Number) row[2]).intValue();
            totalAmount += amount;
            totalCount += count;
            switch (method != null ? method : "") {
                case "카드" -> {
                    cardAmount = amount;
                    cardCount = count;
                }
                case "간편결제" -> {
                    easyPayAmount = amount;
                    easyPayCount = count;
                }
                case "계좌이체" -> {
                    transferAmount = amount;
                    transferCount = count;
                }
                case "휴대폰" -> {
                    phoneAmount = amount;
                    phoneCount = count;
                }
            }
        }

        PaymentAnalysisResDto.PaymentMethodBreakdown breakdown = PaymentAnalysisResDto.PaymentMethodBreakdown.builder()
                .cardRate(calcRate(cardCount, totalCount))
                .easyPayRate(calcRate(easyPayCount, totalCount))
                .transferRate(calcRate(transferCount, totalCount))
                .phoneRate(calcRate(phoneCount, totalCount))
                .build();

        // ★ 월 누적 총액 (특정 일 선택 시에도 월 전체 누적 표시용)
        int monthlyTotalAmount;
        if ("monthly".equals(dto.getViewMode()) || (dto.getDay() == 0 && !"weekly".equals(dto.getViewMode()))) {
            // 월별 조회이거나, 일별+전체(day=0)면 → 이미 조회한 totalAmount가 월 전체
            monthlyTotalAmount = totalAmount;
        } else {
            // 일별(특정일) 또는 주별 → 월 누적은 별도 조회
            LocalDateTime monthStartAt = ym.atDay(1).atStartOfDay();
            LocalDateTime monthEndAt = ym.plusMonths(1).atDay(1).atStartOfDay();
            List<Object[]> monthRaw = paymentRepository.sumByPaymentMethod(storeId, monthStartAt, monthEndAt);
            monthlyTotalAmount = monthRaw.stream().mapToInt(r -> ((Number) r[1]).intValue()).sum();
        }

        PaymentAnalysisResDto.PaymentSummary summary = PaymentAnalysisResDto.PaymentSummary.builder()
                .avgAmount(totalCount > 0 ? totalAmount / totalCount : 0)
                .totalCount(totalCount)
                .totalAmount(totalAmount)
                .monthlyTotal(monthlyTotalAmount)
                .build();

        // ★ 결제 내역도 선택된 기간 기준으로 조회
        List<Payment> recentPayments = paymentRepository.findRecentTransactions(storeId, startAt, endAt);

        Set<UUID> paymentGroupIds = recentPayments.stream()
                .map(Payment::getGroupId)
                .filter(Objects::nonNull)
                .map(gid -> {
                    try {
                        return UUID.fromString(gid);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, List<Ordering>> groupOrdersMap = batchLoadOrdersByGroupIds(paymentGroupIds);

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        List<PaymentAnalysisResDto.TransactionDetail> transactions = new ArrayList<>();

        for (Payment payment : recentPayments) {
            List<PaymentAnalysisResDto.TransactionMenu> menus = new ArrayList<>();
            if (payment.getGroupId() != null) {
                try {
                    UUID groupId = UUID.fromString(payment.getGroupId());
                    List<Ordering> groupOrders = groupOrdersMap.getOrDefault(groupId, List.of());
                    for (Ordering order : groupOrders) {
                        for (OrderingDetail detail : order.getOrderDetail()) {
                            menus.add(PaymentAnalysisResDto.TransactionMenu.builder()
                                    .name(detail.getMenu().getMenuName())
                                    .qty(detail.getOrderingDetailQuantity())
                                    .price(detail.getMenuPrice() * detail.getOrderingDetailQuantity())
                                    .build());
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            transactions.add(PaymentAnalysisResDto.TransactionDetail.builder()
                    .id(payment.getId())
                    .method(payment.getMethod() != null ? payment.getMethod() : "기타")
                    .time(payment.getApproveAt() != null ? payment.getApproveAt().format(timeFmt) : "")
                    .amount(payment.getAmount())
                    .tableNum(payment.getTableNum() != null ? payment.getTableNum() : 0)
                    .menus(menus).build());
        }

        return PaymentAnalysisResDto.builder()
                .methodBreakdown(breakdown).summary(summary).recentTransactions(transactions).build();
    }

    // ═══════════════════════════════════════════════════════════
    //  테이블 분석
    // ═══════════════════════════════════════════════════════════
    public TableAnalysisResDto getTableAnalysis(Long storeId, PeriodReqDto dto) {
        // ★ 수정: dto.day 반영
        LocalDateTime startAt = dto.getStartAt();
        LocalDateTime endAt = dto.getEndAt();
        YearMonth ym = YearMonth.of(dto.getYear(), dto.getMonth());
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();

        List<Object[]> tableSalesRaw = orderingRepository.sumSalesByTable(storeId, startAt, endAt);
        int maxTableAmount = tableSalesRaw.stream().mapToInt(r -> ((Number) r[1]).intValue()).max().orElse(1);

        List<TableAnalysisResDto.TableSales> tableSales = tableSalesRaw.stream()
                .map(row -> TableAnalysisResDto.TableSales.builder()
                        .tableNum(((Number) row[0]).intValue())
                        .amount(((Number) row[1]).intValue())
                        .count(((Number) row[2]).intValue())
                        .rate(calcRate(((Number) row[1]).intValue(), maxTableAmount))
                        .build())
                .collect(Collectors.toList());

        List<Object[]> turnoverRaw = orderingRepository.countGroupsByTable(storeId, startAt, endAt);
        int daysInMonth = ym.lengthOfMonth();

        // ★ 수정: 특정 일이면 1일, 월 전체면 기존 로직
        int daysElapsed;
        if ("weekly".equals(dto.getViewMode())) {
            LocalDate weekEnd = dto.getWeekEnd();
            daysElapsed = weekEnd.isAfter(today)
                    ? (int) java.time.temporal.ChronoUnit.DAYS.between(dto.getWeekStart(), today) + 1
                    : 7;
        } else if (dto.getDay() > 0) {
            daysElapsed = 1;
        } else {
            daysElapsed = today.getMonthValue() == dto.getMonth() && today.getYear() == dto.getYear()
                    ? today.getDayOfMonth() : daysInMonth;
        }

        List<TableAnalysisResDto.TableTurnover> tableTurnover = turnoverRaw.stream()
                .map(row -> {
                    int groups = ((Number) row[1]).intValue();
                    double turnover = daysElapsed > 0 ? Math.round((double) groups / daysElapsed * 10.0) / 10.0 : 0;
                    return TableAnalysisResDto.TableTurnover.builder()
                            .tableNum(((Number) row[0]).intValue()).turnover(turnover).build();
                })
                .collect(Collectors.toList());

        int periodUseCount = orderingRepository.countDistinctGroupIds(storeId, startAt, endAt);
        double avgTurnover = tableTurnover.stream()
                .mapToDouble(TableAnalysisResDto.TableTurnover::getTurnover).average().orElse(0);
        avgTurnover = Math.round(avgTurnover * 10.0) / 10.0;

        int totalOrders = tableSales.stream().mapToInt(TableAnalysisResDto.TableSales::getCount).sum();

        TableAnalysisResDto.TableSummary summaryDto = TableAnalysisResDto.TableSummary.builder()
                .avgTurnover(avgTurnover).avgDuration(totalOrders > 0 ? 75 : 0).todayUseCount(periodUseCount).build();

        return TableAnalysisResDto.builder()
                .summary(summaryDto).tableSales(tableSales).tableTurnover(tableTurnover).build();
    }
    // ═══════════════════════════════════════════════════════════
    //  공통 헬퍼: Settlement 합산 (Weekly/Monthly 공유)
    // ═══════════════════════════════════════════════════════════

    private SettlementTotals sumSettlements(List<StoreSettlement> settlements) {
        int totalAmount = 0, orderCount = 0, cancelCount = 0, refundAmount = 0, tableUseCount = 0;
        int cardAmount = 0, cardCount = 0, transferAmount = 0, transferCount = 0;
        int easyPayAmount = 0, easyPayCount = 0, phoneAmount = 0, phoneCount = 0;

        for (StoreSettlement ss : settlements) {
            totalAmount += ss.getDayTotalAmount();
            orderCount += ss.getOrderCount();
            cancelCount += ss.getCancelCount();
            refundAmount += ss.getRefundAmount();
            tableUseCount += ss.getTableUseCount();
            cardAmount += ss.getCardAmount();
            cardCount += ss.getCardCount();
            transferAmount += ss.getTransferAmount();
            transferCount += ss.getTransferCount();
            easyPayAmount += ss.getEasyPayAmount();
            easyPayCount += ss.getEasyPayCount();
            phoneAmount += ss.getPhoneAmount();
            phoneCount += ss.getPhoneCount();
        }

        return new SettlementTotals(
                totalAmount, orderCount,
                orderCount > 0 ? totalAmount / orderCount : 0,
                cancelCount, refundAmount, totalAmount - refundAmount, tableUseCount,
                new PaymentMethodAgg(cardAmount, cardCount, transferAmount, transferCount,
                        easyPayAmount, easyPayCount, phoneAmount, phoneCount));
    }

    // ═══════════════════════════════════════════════════════════
    //  공통 헬퍼: 자식 엔티티 집계 (Weekly/Monthly 공유)
    // ═══════════════════════════════════════════════════════════

    private List<MenuAgg> aggregateMenuRanks(List<StoreSettlement> settlements) {
        Map<String, int[]> merged = new LinkedHashMap<>();
        Map<String, String> catMap = new HashMap<>();
        for (StoreSettlement ss : settlements) {
            for (SettlementMenuRank mr : ss.getMenuRanks()) {
                merged.computeIfAbsent(mr.getMenuName(), k -> new int[]{0, 0});
                merged.get(mr.getMenuName())[0] += mr.getSalesCount();
                merged.get(mr.getMenuName())[1] += mr.getSalesAmount();
                catMap.putIfAbsent(mr.getMenuName(), mr.getCategoryName());
            }
        }
        return merged.entrySet().stream()
                .map(e -> new MenuAgg(e.getKey(), catMap.get(e.getKey()), e.getValue()[0], e.getValue()[1]))
                .collect(Collectors.toList());
    }

    private List<CategoryAgg> aggregateCategoryRanks(List<StoreSettlement> settlements) {
        Map<String, int[]> merged = new LinkedHashMap<>();
        for (StoreSettlement ss : settlements) {
            for (SettlementCategoryRank cr : ss.getCategoryRanks()) {
                merged.computeIfAbsent(cr.getCategoryName(), k -> new int[]{0, 0});
                merged.get(cr.getCategoryName())[0] += cr.getSalesCount();
                merged.get(cr.getCategoryName())[1] += cr.getSalesAmount();
            }
        }
        return merged.entrySet().stream()
                .map(e -> new CategoryAgg(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .collect(Collectors.toList());
    }

    private List<TableAgg> aggregateTableStats(List<StoreSettlement> settlements) {
        Map<Integer, int[]> merged = new LinkedHashMap<>();
        for (StoreSettlement ss : settlements) {
            for (SettlementTableStat ts : ss.getTableStats()) {
                merged.computeIfAbsent(ts.getTableNum(), k -> new int[]{0, 0, 0, 0});
                merged.get(ts.getTableNum())[0] += ts.getUseCount();
                merged.get(ts.getTableNum())[1] += ts.getSalesAmount();
                merged.get(ts.getTableNum())[2] += ts.getOrderCount();
                merged.get(ts.getTableNum())[3] += ts.getAvgUsageMinutes();
            }
        }
        int days = Math.max(settlements.size(), 1);
        return merged.entrySet().stream()
                .map(e -> new TableAgg(e.getKey(), e.getValue()[0], e.getValue()[1],
                        e.getValue()[2], e.getValue()[3] / days))
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════
    //  공통 Agg → DTO 변환
    // ═══════════════════════════════════════════════════════════

    private List<WeeklySettlementResDto.MenuRankItem> toWeeklyMenuRank(List<MenuAgg> aggs, boolean byCount) {
        List<MenuAgg> sorted = new ArrayList<>(aggs);
        sorted.sort(byCount
                ? Comparator.comparingInt(MenuAgg::salesCount).reversed()
                : Comparator.comparingInt(MenuAgg::salesAmount).reversed());
        List<WeeklySettlementResDto.MenuRankItem> result = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            MenuAgg a = sorted.get(i);
            result.add(WeeklySettlementResDto.MenuRankItem.builder()
                    .rank(i + 1).menuName(a.menuName).categoryName(a.categoryName)
                    .salesCount(a.salesCount).salesAmount(a.salesAmount).build());
        }
        return result;
    }

    private List<WeeklySettlementResDto.CategoryRankItem> toWeeklyCategoryRank(List<CategoryAgg> aggs) {
        List<CategoryAgg> sorted = new ArrayList<>(aggs);
        sorted.sort(Comparator.comparingInt(CategoryAgg::salesAmount).reversed());
        List<WeeklySettlementResDto.CategoryRankItem> result = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            CategoryAgg a = sorted.get(i);
            result.add(WeeklySettlementResDto.CategoryRankItem.builder()
                    .rank(i + 1).categoryName(a.categoryName)
                    .salesCount(a.salesCount).salesAmount(a.salesAmount).build());
        }
        return result;
    }

    private List<WeeklySettlementResDto.TableStatItem> toWeeklyTableStats(List<TableAgg> aggs) {
        return aggs.stream()
                .sorted(Comparator.comparingInt(TableAgg::salesAmount).reversed())
                .map(a -> WeeklySettlementResDto.TableStatItem.builder()
                        .tableNum(a.tableNum).useCount(a.useCount)
                        .salesAmount(a.salesAmount).orderCount(a.orderCount)
                        .avgUsageMinutes(a.avgUsageMinutes).build())
                .collect(Collectors.toList());
    }

    private List<MonthlySettlementResDto.MenuRankItem> toMonthlyMenuRank(List<MenuAgg> aggs, boolean byCount) {
        List<MenuAgg> sorted = new ArrayList<>(aggs);
        sorted.sort(byCount
                ? Comparator.comparingInt(MenuAgg::salesCount).reversed()
                : Comparator.comparingInt(MenuAgg::salesAmount).reversed());
        List<MonthlySettlementResDto.MenuRankItem> result = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            MenuAgg a = sorted.get(i);
            result.add(MonthlySettlementResDto.MenuRankItem.builder()
                    .rank(i + 1).menuName(a.menuName).categoryName(a.categoryName)
                    .salesCount(a.salesCount).salesAmount(a.salesAmount).build());
        }
        return result;
    }

    private List<MonthlySettlementResDto.CategoryRankItem> toMonthlyCategoryRank(List<CategoryAgg> aggs) {
        List<CategoryAgg> sorted = new ArrayList<>(aggs);
        sorted.sort(Comparator.comparingInt(CategoryAgg::salesAmount).reversed());
        List<MonthlySettlementResDto.CategoryRankItem> result = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            CategoryAgg a = sorted.get(i);
            result.add(MonthlySettlementResDto.CategoryRankItem.builder()
                    .rank(i + 1).categoryName(a.categoryName)
                    .salesCount(a.salesCount).salesAmount(a.salesAmount).build());
        }
        return result;
    }

    private List<MonthlySettlementResDto.TableStatItem> toMonthlyTableStats(List<TableAgg> aggs) {
        return aggs.stream()
                .sorted(Comparator.comparingInt(TableAgg::salesAmount).reversed())
                .map(a -> MonthlySettlementResDto.TableStatItem.builder()
                        .tableNum(a.tableNum).useCount(a.useCount)
                        .salesAmount(a.salesAmount).orderCount(a.orderCount)
                        .avgUsageMinutes(a.avgUsageMinutes).build())
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════
    //  Daily 전용 헬퍼
    // ═══════════════════════════════════════════════════════════

    private List<DailySettlementResDto.MenuRankItem> buildMenuRank(
            Map<String, int[]> menuMap, Map<String, String> menuCategoryMap, boolean byCount) {
        List<Map.Entry<String, int[]>> sorted = new ArrayList<>(menuMap.entrySet());
        sorted.sort(byCount
                ? (a, b) -> Integer.compare(b.getValue()[0], a.getValue()[0])
                : (a, b) -> Integer.compare(b.getValue()[1], a.getValue()[1]));
        List<DailySettlementResDto.MenuRankItem> result = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, int[]> e = sorted.get(i);
            result.add(DailySettlementResDto.MenuRankItem.builder()
                    .rank(i + 1).menuName(e.getKey()).categoryName(menuCategoryMap.get(e.getKey()))
                    .salesCount(e.getValue()[0]).salesAmount(e.getValue()[1]).build());
        }
        return result;
    }

    private List<DailySettlementResDto.CategoryRankItem> buildCategoryRank(Map<String, int[]> catMap) {
        List<Map.Entry<String, int[]>> sorted = new ArrayList<>(catMap.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue()[1], a.getValue()[1]));
        List<DailySettlementResDto.CategoryRankItem> result = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, int[]> e = sorted.get(i);
            result.add(DailySettlementResDto.CategoryRankItem.builder()
                    .rank(i + 1).categoryName(e.getKey())
                    .salesCount(e.getValue()[0]).salesAmount(e.getValue()[1]).build());
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════════
    //  N+1 해결용 Batch 로더
    // ═══════════════════════════════════════════════════════════

    private Map<String, String> batchLoadPaymentMethods(Set<String> groupIds) {
        if (groupIds.isEmpty()) return Map.of();
        try {
            return paymentRepository.findByGroupIdIn(groupIds).stream()
                    .filter(p -> p.getGroupId() != null && p.getMethod() != null)
                    .collect(Collectors.toMap(Payment::getGroupId, Payment::getMethod, (a, b) -> a));
        } catch (Exception e) {
            log.warn("결제수단 batch 조회 실패: {}", e.getMessage());
            return Map.of();
        }
    }

    private Map<UUID, List<Ordering>> batchLoadOrdersByGroupIds(Set<UUID> groupIds) {
        if (groupIds.isEmpty()) return Map.of();
        try {
            return orderingRepository.findByGroupIdIn(groupIds).stream()
                    .collect(Collectors.groupingBy(Ordering::getGroupId));
        } catch (Exception e) {
            log.warn("주문 batch 조회 실패: {}", e.getMessage());
            return Map.of();
        }
    }

    private PaymentMethodAgg aggregatePaymentMethods(Long storeId, LocalDateTime startAt, LocalDateTime endAt) {
        int cardAmount = 0, cardCount = 0, transferAmount = 0, transferCount = 0;
        int easyPayAmount = 0, easyPayCount = 0, phoneAmount = 0, phoneCount = 0;
        try {
            for (Object[] row : paymentRepository.sumByPaymentMethod(storeId, startAt, endAt)) {
                String method = (String) row[0];
                int amount = ((Number) row[1]).intValue();
                int count = ((Number) row[2]).intValue();
                if (method == null) continue;
                switch (method) {
                    case "카드" -> {
                        cardAmount = amount;
                        cardCount = count;
                    }
                    case "계좌이체" -> {
                        transferAmount = amount;
                        transferCount = count;
                    }
                    case "간편결제" -> {
                        easyPayAmount = amount;
                        easyPayCount = count;
                    }
                    case "휴대폰" -> {
                        phoneAmount = amount;
                        phoneCount = count;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("결제수단 집계 실패(매장ID: {}): {}", storeId, e.getMessage());
        }
        return new PaymentMethodAgg(cardAmount, cardCount, transferAmount, transferCount,
                easyPayAmount, easyPayCount, phoneAmount, phoneCount);
    }

    // ═══════════════════════════════════════════════════════════
    //  매출 분석 차트 헬퍼
    // ═══════════════════════════════════════════════════════════

    /**
     * ★ 12개월 개별 쿼리 → 연간 1회 쿼리
     */
    private List<SalesAnalysisResDto.SalesBar> buildMonthlyBarsOptimized(Long storeId, int year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year + 1, 1, 1);

        List<StoreSettlement> yearSettlements = storeSettlementRepository
                .findByStoreIdAndSettlementDateRange(storeId, yearStart, yearEnd);

        int[] monthlyValues = new int[12];
        for (StoreSettlement ss : yearSettlements) {
            monthlyValues[ss.getSettlementDate().getMonthValue() - 1] += ss.getDayTotalAmount();
        }

        int max = Arrays.stream(monthlyValues).max().orElse(1);
        List<SalesAnalysisResDto.SalesBar> bars = new ArrayList<>(12);
        for (int m = 0; m < 12; m++) {
            bars.add(SalesAnalysisResDto.SalesBar.builder()
                    .label((m + 1) + "월").value(monthlyValues[m])
                    .height(calcRate(monthlyValues[m], max))
                    .best(monthlyValues[m] == max && max > 0).build());
        }
        return bars;
    }

    /**
     * ★ 이번달 settlements 재활용, 전월/전년만 추가 조회
     */
    private SalesAnalysisResDto.SalesCompare buildSalesCompareOptimized(
            Long storeId, int year, int month, List<StoreSettlement> currentMonthSettlements) {
        int thisMonth = currentMonthSettlements.stream().mapToInt(StoreSettlement::getDayTotalAmount).sum();
        YearMonth prev = YearMonth.of(year, month).minusMonths(1);
        int lastMonth = getMonthTotal(storeId, prev.getYear(), prev.getMonthValue());
        int lastYear = getMonthTotal(storeId, year - 1, month);

        double momRate = lastMonth > 0 ? Math.round((double) (thisMonth - lastMonth) / lastMonth * 1000.0) / 10.0 : 0;
        double yoyRate = lastYear > 0 ? Math.round((double) (thisMonth - lastYear) / lastYear * 1000.0) / 10.0 : 0;
        return SalesAnalysisResDto.SalesCompare.builder()
                .thisMonth(thisMonth).lastMonth(lastMonth).momRate(momRate)
                .lastYear(lastYear).yoyRate(yoyRate).build();
    }

    private int getMonthTotal(Long storeId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        return storeSettlementRepository
                .findByStoreIdAndSettlementDateRange(storeId, ym.atDay(1), ym.plusMonths(1).atDay(1))
                .stream().mapToInt(StoreSettlement::getDayTotalAmount).sum();
    }

    private List<SalesAnalysisResDto.SalesBar> buildDailyBars(List<StoreSettlement> settlements) {
        if (settlements.isEmpty()) return Collections.emptyList();
        int max = settlements.stream().mapToInt(StoreSettlement::getDayTotalAmount).max().orElse(1);
        return settlements.stream()
                .map(ss -> SalesAnalysisResDto.SalesBar.builder()
                        .label(ss.getSettlementDate().getDayOfMonth() + "일")
                        .value(ss.getDayTotalAmount())
                        .height(calcRate(ss.getDayTotalAmount(), max))
                        .best(ss.getDayTotalAmount() == max).build())
                .collect(Collectors.toList());
    }

    private List<SalesAnalysisResDto.SalesBar> buildWeeklyBars(List<StoreSettlement> settlements) {
        Map<Integer, Integer> weekMap = new LinkedHashMap<>();
        for (StoreSettlement ss : settlements) {
            int week = (ss.getSettlementDate().getDayOfMonth() - 1) / 7 + 1;
            weekMap.merge(week, ss.getDayTotalAmount(), Integer::sum);
        }
        int max = weekMap.values().stream().mapToInt(v -> v).max().orElse(1);
        return weekMap.entrySet().stream()
                .map(e -> SalesAnalysisResDto.SalesBar.builder()
                        .label(e.getKey() + "주").value(e.getValue())
                        .height(calcRate(e.getValue(), max))
                        .best(e.getValue() == max).build())
                .collect(Collectors.toList());
    }

    private List<SalesAnalysisResDto.HourlySales> buildHourlySales(List<Object[]> raw) {
        Map<Integer, Integer> hourMap = new LinkedHashMap<>();
        for (Object[] row : raw) {
            hourMap.put(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
        }
        int max = hourMap.values().stream().mapToInt(v -> v).max().orElse(1);
        List<SalesAnalysisResDto.HourlySales> result = new ArrayList<>(24);
        for (int h = 0; h < 24; h++) {
            int val = hourMap.getOrDefault(h, 0);
            result.add(SalesAnalysisResDto.HourlySales.builder()
                    .hour(h).amount(val).height(calcRate(val, max)).build());
        }
        return result;
    }

    private List<SalesAnalysisResDto.SalesBar> buildDayOfWeekSales(List<Object[]> raw) {
        String[] labels = {"일", "월", "화", "수", "목", "금", "토"};
        int[] values = new int[7];
        for (Object[] row : raw) {
            values[((Number) row[0]).intValue() - 1] = ((Number) row[1]).intValue();
        }
        int max = Arrays.stream(values).max().orElse(1);
        List<SalesAnalysisResDto.SalesBar> result = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            result.add(SalesAnalysisResDto.SalesBar.builder()
                    .label(labels[i]).value(values[i])
                    .height(calcRate(values[i], max))
                    .best(values[i] == max && max > 0).build());
        }
        return result;
    }

    private List<MonthlySettlementResDto.WeeklyItem> buildWeeklyBreakdown(List<StoreSettlement> settlements, int year, int month) {
        Map<Integer, int[]> weekMap = new LinkedHashMap<>();
        Map<Integer, LocalDate[]> weekDateMap = new LinkedHashMap<>();

        for (StoreSettlement ss : settlements) {
            LocalDate date = ss.getSettlementDate();
            LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            int weekNum = (date.getDayOfMonth() - 1) / 7 + 1;

            weekMap.computeIfAbsent(weekNum, k -> new int[]{0, 0});
            weekMap.get(weekNum)[0] += ss.getDayTotalAmount();
            weekMap.get(weekNum)[1] += ss.getOrderCount();
            weekDateMap.computeIfAbsent(weekNum, k -> new LocalDate[]{weekStart, weekStart.plusDays(6)});
        }

        return weekMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> MonthlySettlementResDto.WeeklyItem.builder()
                        .week(e.getKey())
                        .weekStart(weekDateMap.get(e.getKey())[0])
                        .weekEnd(weekDateMap.get(e.getKey())[1])
                        .totalAmount(e.getValue()[0]).orderCount(e.getValue()[1]).build())
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════
    //  공통 유틸
    // ═══════════════════════════════════════════════════════════

    private static int calcRate(int value, int total) {
        return total > 0 ? (int) ((long) value * 100 / total) : 0;
    }

    private List<MenuAnalysisResDto.MenuCombo> buildCombos(Map<UUID, Set<String>> groupMenus) {
        Map<String, Integer> comboMap = new HashMap<>();
        for (Set<String> menus : groupMenus.values()) {
            List<String> menuList = new ArrayList<>(menus);
            Collections.sort(menuList);
            for (int i = 0; i < menuList.size(); i++) {
                for (int j = i + 1; j < menuList.size(); j++) {
                    comboMap.merge(menuList.get(i) + " + " + menuList.get(j), 1, Integer::sum);
                }
            }
        }
        return comboMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(e -> MenuAnalysisResDto.MenuCombo.builder().pair(e.getKey()).count(e.getValue()).build())
                .collect(Collectors.toList());
    }
}