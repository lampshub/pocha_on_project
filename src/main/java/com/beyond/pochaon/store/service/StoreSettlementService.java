package com.beyond.pochaon.store.service;

import com.beyond.pochaon.ingredient.domain.IngredientDetail;
import com.beyond.pochaon.ingredient.domain.IngredientLoss;
import com.beyond.pochaon.ingredient.repository.IngredientDetailRepository;
import com.beyond.pochaon.ingredient.repository.IngredientLossRepository;
import com.beyond.pochaon.ingredient.repository.IngredientMenuRepository;
import com.beyond.pochaon.ordering.domain.Ordering;
import com.beyond.pochaon.ordering.domain.OrderingDetail;
import com.beyond.pochaon.ordering.repository.OrderingRepository;
import com.beyond.pochaon.payment.entity.Payment;
import com.beyond.pochaon.payment.repository.PaymentRepository;
import com.beyond.pochaon.store.common.ProfitCalculator;
import com.beyond.pochaon.store.dto.*;
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

    private final OrderingRepository orderingRepository;
    private final PaymentRepository paymentRepository;
    private final IngredientMenuRepository ingredientMenuRepository;
    private final IngredientDetailRepository ingredientDetailRepository;
    private final IngredientLossRepository ingredientLossRepository;

    @Autowired
    public StoreSettlementService(OrderingRepository orderingRepository,
                                  PaymentRepository paymentRepository,
                                  IngredientMenuRepository ingredientMenuRepository,
                                  IngredientDetailRepository ingredientDetailRepository,
                                  IngredientLossRepository ingredientLossRepository) {
        this.orderingRepository = orderingRepository;
        this.paymentRepository = paymentRepository;
        this.ingredientMenuRepository = ingredientMenuRepository;
        this.ingredientDetailRepository = ingredientDetailRepository;
        this.ingredientLossRepository = ingredientLossRepository;
    }

    // ═══════════════════════════════════════════════════════════
    //  공통 내부 레코드
    // ═══════════════════════════════════════════════════════════

    private record PaymentMethodAgg(
            int cardAmount, int cardCount, int transferAmount, int transferCount,
            int easyPayAmount, int easyPayCount, int phoneAmount, int phoneCount) {}

    private record ProfitCalc(
            int totalCost, int grossProfit, double grossProfitRate,
            int vat, int netProfit, double netProfitRate) {}

    private record BasicAgg(
            int totalAmount, int orderCount, int averageOrderAmount,
            int cancelCount, int refundAmount, int netSales, int tableUseCount) {}

    /** 주문 기반 메뉴/카테고리/테이블 + 원가 집계 결과 */
    private record OrderAggregation(
            Map<String, int[]> menuMap,          // menuName → [수량, 매출, 원가]
            Map<String, String> menuCategoryMap,  // menuName → categoryName
            Map<String, int[]> catMap,            // categoryName → [수량, 매출]
            Map<Integer, int[]> tableMap,         // tableNum → [이용횟수, 매출, 주문수]
            int totalCost) {}

    // ═══════════════════════════════════════════════════════════
    //  공통 집계 메서드
    // ═══════════════════════════════════════════════════════════

    /** 기간별 기본 수치 (매출, 주문수, 취소, 테이블이용) */
    private BasicAgg aggregateBasic(Long storeId, LocalDateTime startAt, LocalDateTime endAt) {
        int totalAmount = Math.max(orderingRepository.sumTotalRevenue(storeId, startAt, endAt), 0);
        int orderCount = orderingRepository.countCompletedOrders(storeId, startAt, endAt);
        int cancelCount = orderingRepository.countCancelledOrders(storeId, startAt, endAt);
        int tableUseCount = orderingRepository.countDistinctGroupIds(storeId, startAt, endAt);
        int avgOrder = orderCount > 0 ? totalAmount / orderCount : 0;
        return new BasicAgg(totalAmount, orderCount, avgOrder, cancelCount, 0, totalAmount, tableUseCount);
    }

    /** 주문 목록에서 메뉴/카테고리/테이블 + 원가를 한 번에 집계 */
    private OrderAggregation aggregateOrders(List<Ordering> orders, Map<Long, Integer> menuCostMap) {
        Map<String, int[]> menuMap = new LinkedHashMap<>();
        Map<String, String> menuCatMap = new HashMap<>();
        Map<String, int[]> catMap = new LinkedHashMap<>();
        Map<Integer, int[]> tableMap = new LinkedHashMap<>();
        Set<String> tableGroupSet = new HashSet<>();
        int totalCost = 0;

        for (Ordering order : orders) {
            for (OrderingDetail d : order.getOrderDetail()) {
                String menuName = d.getMenu().getMenuName();
                String catName = d.getMenu().getCategory().getCategoryName();
                int qty = d.getOrderingDetailQuantity();
                int amount = d.getMenuPrice() * qty;
                int cost = menuCostMap.getOrDefault(d.getMenu().getId(), 0) * qty;
                totalCost += cost;

                menuMap.computeIfAbsent(menuName, k -> new int[]{0, 0, 0});
                menuMap.get(menuName)[0] += qty;
                menuMap.get(menuName)[1] += amount;
                menuMap.get(menuName)[2] += cost;
                menuCatMap.putIfAbsent(menuName, catName);

                catMap.computeIfAbsent(catName, k -> new int[]{0, 0});
                catMap.get(catName)[0] += qty;
                catMap.get(catName)[1] += amount;
            }

            int tNum = order.getCustomerTable().getTableNum();
            tableMap.computeIfAbsent(tNum, k -> new int[]{0, 0, 0});
            tableMap.get(tNum)[1] += order.getTotalPrice();
            tableMap.get(tNum)[2] += 1;
            if (order.getGroupId() != null) {
                if (tableGroupSet.add(tNum + ":" + order.getGroupId())) {
                    tableMap.get(tNum)[0] += 1;
                }
            }
        }
        return new OrderAggregation(menuMap, menuCatMap, catMap, tableMap, totalCost);
    }

    /** 이익/세금 계산 */
    private ProfitCalc calculateProfit(int totalAmount, int totalCost) {
        int grossProfit = ProfitCalculator.grossProfit(totalAmount, totalCost);
        int vat = ProfitCalculator.vat(totalAmount);
        int netProfit = ProfitCalculator.netProfit(totalAmount, totalCost);
        return new ProfitCalc(totalCost, grossProfit, pctRate(grossProfit, totalAmount),
                vat, netProfit, pctRate(netProfit, totalAmount));
    }

    /** 매장의 메뉴별 원가 Map (menuId → 1개당 원가) */
    private Map<Long, Integer> loadMenuCostMap(Long storeId) {
        Map<Long, Integer> map = new HashMap<>();
        try {
            for (Object[] row : ingredientMenuRepository.findMenuCostByStoreId(storeId)) {
                map.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
            }
        } catch (Exception e) {
            log.warn("원가 조회 실패(매장ID: {}): {}", storeId, e.getMessage());
        }
        return map;
    }

    /** 결제수단별 집계 */
    private PaymentMethodAgg aggregatePayments(Long storeId, LocalDateTime startAt, LocalDateTime endAt) {
        int cA = 0, cC = 0, tA = 0, tC = 0, eA = 0, eC = 0, pA = 0, pC = 0;
        try {
            for (Object[] row : paymentRepository.sumByPaymentMethod(storeId, startAt, endAt)) {
                String m = (String) row[0];
                int amt = ((Number) row[1]).intValue();
                int cnt = ((Number) row[2]).intValue();
                if (m == null) continue;
                switch (m) {
                    case "카드"   -> { cA = amt; cC = cnt; }
                    case "계좌이체" -> { tA = amt; tC = cnt; }
                    case "간편결제" -> { eA = amt; eC = cnt; }
                    case "휴대폰"  -> { pA = amt; pC = cnt; }
                }
            }
        } catch (Exception e) {
            log.warn("결제수단 집계 실패(매장ID: {}): {}", storeId, e.getMessage());
        }
        return new PaymentMethodAgg(cA, cC, tA, tC, eA, eC, pA, pC);
    }

    /** 특정 월 총매출 (실시간) */
    private int getMonthTotal(Long storeId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        return Math.max(orderingRepository.sumTotalRevenue(storeId,
                ym.atDay(1).atStartOfDay(), ym.plusMonths(1).atDay(1).atStartOfDay()), 0);
    }

    // ═══════════════════════════════════════════════════════════
    //  N+1 해결용 Batch 로더
    // ═══════════════════════════════════════════════════════════

    private Map<String, String> batchPaymentMethods(Set<String> groupIds) {
        if (groupIds.isEmpty()) return Map.of();
        try {
            return paymentRepository.findByGroupIdIn(groupIds).stream()
                    .filter(p -> p.getGroupId() != null && p.getMethod() != null)
                    .collect(Collectors.toMap(Payment::getGroupId, Payment::getMethod, (a, b) -> a));
        } catch (Exception e) { return Map.of(); }
    }

    private Map<UUID, List<Ordering>> batchOrdersByGroupIds(Set<UUID> groupIds) {
        if (groupIds.isEmpty()) return Map.of();
        try {
            return orderingRepository.findByGroupIdIn(groupIds).stream()
                    .collect(Collectors.groupingBy(Ordering::getGroupId));
        } catch (Exception e) { return Map.of(); }
    }

    // ═══════════════════════════════════════════════════════════
    //  일별 정산
    // ═══════════════════════════════════════════════════════════

    public DailySettlementResDto getDailySettlement(Long storeId, PeriodReqDto dto) {
        LocalDateTime startAt = dto.getStartAt();
        LocalDateTime endAt = dto.getEndAt();

        BasicAgg basic = aggregateBasic(storeId, startAt, endAt);
        PaymentMethodAgg pma = aggregatePayments(storeId, startAt, endAt);
        Map<Long, Integer> costMap = loadMenuCostMap(storeId);
        List<Ordering> orders = orderingRepository.findCompletedOrdersWithDetails(storeId, startAt, endAt);
        OrderAggregation agg = aggregateOrders(orders, costMap);

        // ★ 실제 재료 입고 비용 우선, 없으면 메뉴 기반 원가
        int ingredientCost = ingredientDetailRepository.sumTotalPriceByStoreAndPeriod(storeId, startAt, endAt);
        int actualCost = ingredientCost > 0 ? ingredientCost : agg.totalCost;
        ProfitCalc profit = calculateProfit(basic.totalAmount, actualCost);

        // groupId → 결제수단 batch
        Set<String> gids = orders.stream().map(Ordering::getGroupId)
                .filter(Objects::nonNull).map(UUID::toString).collect(Collectors.toSet());
        Map<String, String> payMap = batchPaymentMethods(gids);

        // 주문 내역 (완료 + 취소 모두 포함)
        List<DailySettlementResDto.OrderItem> orderItems = new ArrayList<>();
        // 취소 주문도 조회
        List<Ordering> cancelledOrders = orderingRepository.findCancelledOrdersWithDetails(storeId, startAt, endAt);
        List<Ordering> allOrders = new ArrayList<>(orders);
        allOrders.addAll(cancelledOrders);
        allOrders.sort((a, b) -> b.getCreateTimeAt().compareTo(a.getCreateTimeAt()));

        for (Ordering order : allOrders) {
            List<DailySettlementResDto.OrderMenuItem> menuItems = new ArrayList<>();
            for (OrderingDetail d : order.getOrderDetail()) {
                List<String> opts = d.getOrderingDetailOptions() != null
                        ? d.getOrderingDetailOptions().stream().map(o -> o.getOrderingOptionName()).toList()
                        : List.of();
                menuItems.add(DailySettlementResDto.OrderMenuItem.builder()
                        .menuName(d.getMenu().getMenuName())
                        .quantity(d.getOrderingDetailQuantity())
                        .price(d.getMenuPrice() * d.getOrderingDetailQuantity())
                        .options(opts).build());
            }
            String pm = order.getGroupId() != null ? payMap.get(order.getGroupId().toString()) : null;
            orderItems.add(DailySettlementResDto.OrderItem.builder()
                    .orderingId(order.getId())
                    .tableNum(order.getCustomerTable().getTableNum())
                    .totalPrice(order.getTotalPrice())
                    .orderStatus(order.getOrderStatus().name())
                    .paymentMethod(pm)
                    .orderedAt(order.getCreateTimeAt())
                    .menus(menuItems).build());
        }

        // 전일 매출 계산
        LocalDate currentDate = LocalDate.of(dto.getYear(), dto.getMonth(), dto.getDay());
        LocalDate prevDate = currentDate.minusDays(1);
        LocalDateTime prevStart = prevDate.atStartOfDay();
        LocalDateTime prevEnd = prevDate.plusDays(1).atStartOfDay();
        int prevDayTotal = Math.max(orderingRepository.sumTotalRevenue(storeId, prevStart, prevEnd), 0);

        return buildDailyDto(dto, basic, pma, profit, agg, orderItems, prevDayTotal);
    }

    // ═══════════════════════════════════════════════════════════
    //  주간 정산
    // ═══════════════════════════════════════════════════════════

    public WeeklySettlementResDto getWeeklySettlement(Long storeId, PeriodReqDto dto) {
        LocalDate weekStart = dto.getWeekStart();
        LocalDate weekEnd = dto.getWeekEnd();
        LocalDateTime startAt = weekStart.atStartOfDay();
        LocalDateTime endAt = weekEnd.plusDays(1).atStartOfDay();

        BasicAgg basic = aggregateBasic(storeId, startAt, endAt);
        PaymentMethodAgg pma = aggregatePayments(storeId, startAt, endAt);
        Map<Long, Integer> costMap = loadMenuCostMap(storeId);
        List<Ordering> orders = orderingRepository.findCompletedOrdersWithDetails(storeId, startAt, endAt);
        OrderAggregation agg = aggregateOrders(orders, costMap);

        // ★ 실제 재료 입고 비용 우선
        int ingredientCost = ingredientDetailRepository.sumTotalPriceByStoreAndPeriod(storeId, startAt, endAt);
        int actualCost = ingredientCost > 0 ? ingredientCost : agg.totalCost;
        ProfitCalc profit = calculateProfit(basic.totalAmount, actualCost);

        // 일별 breakdown
        List<Object[]> dailyRaw = orderingRepository.sumSalesByDate(storeId, startAt, endAt);
        Map<LocalDate, int[]> dailyMap = new HashMap<>();
        for (Object[] row : dailyRaw) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            dailyMap.put(date, new int[]{
                    ((Number) row[1]).intValue(), ((Number) row[2]).intValue(), ((Number) row[3]).intValue()});
        }
        List<WeeklySettlementResDto.DailyItem> dailyBreakdown = new ArrayList<>(7);
        for (LocalDate d = weekStart; !d.isAfter(weekEnd); d = d.plusDays(1)) {
            int[] v = dailyMap.getOrDefault(d, new int[]{0, 0, 0});
            dailyBreakdown.add(WeeklySettlementResDto.DailyItem.builder()
                    .date(d).dayOfWeek(d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN))
                    .totalAmount(v[0]).orderCount(v[1]).cancelCount(v[2]).build());
        }

        // 최근 5주 트렌드 (현재 주 포함 + 과거 4주)
        List<WeeklySettlementResDto.WeekTrendItem> weeklyTrend = buildWeeklyTrend(storeId, weekStart);

        return buildWeeklyDto(weekStart, weekEnd, basic, pma, profit, agg, dailyBreakdown, weeklyTrend);
    }

    // ═══════════════════════════════════════════════════════════
    //  월별 정산
    // ═══════════════════════════════════════════════════════════

    public MonthlySettlementResDto getMonthlySettlement(Long storeId, PeriodReqDto dto) {
        LocalDateTime startAt = dto.getMonthStart().atStartOfDay();
        LocalDateTime endAt = dto.getMonthEnd().atStartOfDay();

        BasicAgg basic = aggregateBasic(storeId, startAt, endAt);
        PaymentMethodAgg pma = aggregatePayments(storeId, startAt, endAt);
        Map<Long, Integer> costMap = loadMenuCostMap(storeId);
        List<Ordering> orders = orderingRepository.findCompletedOrdersWithDetails(storeId, startAt, endAt);
        OrderAggregation agg = aggregateOrders(orders, costMap);

        // ★ 실제 재료 입고 비용 우선, 없으면 메뉴 기반 원가
        int ingredientCost = ingredientDetailRepository.sumTotalPriceByStoreAndPeriod(storeId, startAt, endAt);
        int actualCost = ingredientCost > 0 ? ingredientCost : agg.totalCost;
        ProfitCalc profit = calculateProfit(basic.totalAmount, actualCost);

        // 일별 매출 (캘린더 + 주간 breakdown)
        List<Object[]> dailyRaw = orderingRepository.sumSalesByDate(storeId, startAt, endAt);
        Map<Integer, Integer> dailySales = new HashMap<>();
        int daysWithSales = 0;
        for (Object[] row : dailyRaw) {
            int revenue = ((Number) row[1]).intValue();
            if (revenue > 0) daysWithSales++;
            dailySales.put(((java.sql.Date) row[0]).toLocalDate().getDayOfMonth(), revenue);
        }
        int dailyAvg = daysWithSales > 0 ? basic.totalAmount / daysWithSales : 0;

        // ★ 일별 순이익 — 실제 재료 입고 비용 기반
        Map<Integer, Integer> dailyIngredientCost = new HashMap<>();
        List<Object[]> dailyCostRaw = ingredientDetailRepository.sumDailyCostByStore(storeId, startAt, endAt);
        for (Object[] row : dailyCostRaw) {
            int day = ((java.sql.Date) row[0]).toLocalDate().getDayOfMonth();
            int cost = ((Number) row[1]).intValue();
            dailyIngredientCost.put(day, cost);
        }

        // 입고 데이터 없으면 메뉴 기반 원가로 fallback
        boolean useIngredientCost = !dailyIngredientCost.isEmpty();
        Map<Integer, Integer> dailyMenuCost = new HashMap<>();
        if (!useIngredientCost) {
            for (Ordering order : orders) {
                int dayOfMonth = order.getCreateTimeAt().getDayOfMonth();
                int orderCost = 0;
                for (OrderingDetail d : order.getOrderDetail()) {
                    Integer unitCost = costMap.get(d.getMenu().getId());
                    if (unitCost != null) {
                        orderCost += unitCost * d.getOrderingDetailQuantity();
                    }
                }
                dailyMenuCost.merge(dayOfMonth, orderCost, Integer::sum);
            }
        }

        Map<Integer, Integer> dailyNetProfit = new HashMap<>();
        for (Map.Entry<Integer, Integer> e : dailySales.entrySet()) {
            int rev = e.getValue();
            int cost = useIngredientCost
                    ? dailyIngredientCost.getOrDefault(e.getKey(), 0)
                    : dailyMenuCost.getOrDefault(e.getKey(), 0);
            dailyNetProfit.put(e.getKey(), ProfitCalculator.netProfit(rev, cost));
        }

        // 주간 breakdown
        List<MonthlySettlementResDto.WeeklyItem> weeklyBreakdown = buildWeeklyBreakdown(dailyRaw);

        // 최근 12개월 트렌드 (이번달 포함 + 과거 11개월)
        List<MonthlySettlementResDto.MonthTrendItem> monthlyTrend = buildMonthlyTrend(storeId, dto.getYear(), dto.getMonth());

        return buildMonthlyDto(dto, basic, pma, profit, agg, dailySales, dailyNetProfit, dailyAvg, weeklyBreakdown, monthlyTrend);
    }

    // ═══════════════════════════════════════════════════════════
    //  메뉴 분석
    // ═══════════════════════════════════════════════════════════

    public MenuAnalysisResDto getMenuAnalysis(Long storeId, PeriodReqDto dto) {
        List<Ordering> orders = orderingRepository.findCompletedOrdersWithDetails(storeId, dto.getStartAt(), dto.getEndAt());

        Map<String, Integer> catAmountMap = new LinkedHashMap<>();
        Map<String, int[]> menuMap = new LinkedHashMap<>(); // [수량, 매출]
        Map<UUID, Set<String>> groupMenus = new HashMap<>();

        for (Ordering order : orders) {
            Set<String> names = new HashSet<>();
            for (OrderingDetail d : order.getOrderDetail()) {
                String menuName = d.getMenu().getMenuName();
                String catName = d.getMenu().getCategory().getCategoryName();
                int qty = d.getOrderingDetailQuantity();
                int amount = d.getMenuPrice() * qty;
                catAmountMap.merge(catName, amount, Integer::sum);
                menuMap.computeIfAbsent(menuName, k -> new int[]{0, 0});
                menuMap.get(menuName)[0] += qty;
                menuMap.get(menuName)[1] += amount;
                names.add(menuName);
            }
            if (order.getGroupId() != null)
                groupMenus.computeIfAbsent(order.getGroupId(), k -> new HashSet<>()).addAll(names);
        }

        int maxCat = catAmountMap.values().stream().mapToInt(v -> v).max().orElse(1);
        List<MenuAnalysisResDto.CategorySales> catSales = catAmountMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> MenuAnalysisResDto.CategorySales.builder()
                        .name(e.getKey()).amount(e.getValue()).rate(intRate(e.getValue(), maxCat)).build())
                .collect(Collectors.toList());

        int totalMenuAmt = menuMap.values().stream().mapToInt(v -> v[1]).sum();
        List<MenuAnalysisResDto.MenuRank> menuRank = menuMap.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue()[1], a.getValue()[1]))
                .map(e -> MenuAnalysisResDto.MenuRank.builder()
                        .name(e.getKey()).qty(e.getValue()[0]).amount(e.getValue()[1])
                        .rate(intRate(e.getValue()[1], totalMenuAmt)).build())
                .collect(Collectors.toList());

        return MenuAnalysisResDto.builder()
                .categorySales(catSales).menuRanking(menuRank).combos(buildCombos(groupMenus)).build();
    }

    // ═══════════════════════════════════════════════════════════
    //  매출 분석
    // ═══════════════════════════════════════════════════════════

    public SalesAnalysisResDto getSalesAnalysis(Long storeId, PeriodReqDto dto) {
        String period = dto.getPeriod() != null ? dto.getPeriod() : "weekly";

        List<SalesAnalysisResDto.SalesBar> weeklyBars = null, monthlyBars = null, dayOfWeekSales = null;
        List<SalesAnalysisResDto.HourlySales> hourlySales = null;

        // dto.getStartAt() / getEndAt() 는 startDate/endDate 우선, 없으면 year/month/day 기반
        LocalDateTime rangeStart = dto.getStartAt();
        LocalDateTime rangeEnd   = dto.getEndAt();

        switch (period) {
            case "weekly" -> {
                // 선택 범위 내 일별 매출을 주 단위로 묶기
                List<Object[]> raw = orderingRepository.sumSalesByDate(storeId, rangeStart, rangeEnd);
                Map<LocalDate, Integer> weekMap = new LinkedHashMap<>();

                // 범위 시작~끝까지 주 슬롯 미리 생성
                LocalDate firstMon = rangeStart.toLocalDate()
                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate lastMon  = rangeEnd.toLocalDate().minusDays(1)
                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                for (LocalDate m = firstMon; !m.isAfter(lastMon); m = m.plusWeeks(1)) {
                    weekMap.put(m, 0);
                }

                for (Object[] r : raw) {
                    LocalDate date = ((java.sql.Date) r[0]).toLocalDate();
                    int amount = ((Number) r[1]).intValue();
                    LocalDate weekMon = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    weekMap.merge(weekMon, amount, Integer::sum);
                }

                int maxW = weekMap.values().stream().mapToInt(v -> v).max().orElse(1);
                weeklyBars = new ArrayList<>();
                for (Map.Entry<LocalDate, Integer> entry : weekMap.entrySet()) {
                    LocalDate ws = entry.getKey();
                    LocalDate we = ws.plusDays(6);
                    String label = ws.getMonthValue() + "/" + ws.getDayOfMonth()
                            + "~" + we.getMonthValue() + "/" + we.getDayOfMonth();
                    int val = entry.getValue();
                    weeklyBars.add(SalesAnalysisResDto.SalesBar.builder()
                            .label(label).value(val)
                            .height(intRate(val, maxW)).best(val == maxW && maxW > 0).build());
                }
            }
            case "monthly" -> {
                // 선택 범위 내 일별 매출을 월 단위로 묶기
                List<Object[]> raw = orderingRepository.sumSalesByDate(storeId, rangeStart, rangeEnd);
                Map<YearMonth, Integer> monthMap = new LinkedHashMap<>();

                // 범위 시작~끝까지 월 슬롯 미리 생성
                YearMonth startYm = YearMonth.from(rangeStart.toLocalDate());
                YearMonth endYm   = YearMonth.from(rangeEnd.toLocalDate().minusDays(1));
                for (YearMonth ym = startYm; !ym.isAfter(endYm); ym = ym.plusMonths(1)) {
                    monthMap.put(ym, 0);
                }

                for (Object[] r : raw) {
                    LocalDate date = ((java.sql.Date) r[0]).toLocalDate();
                    YearMonth ym = YearMonth.from(date);
                    monthMap.merge(ym, ((Number) r[1]).intValue(), Integer::sum);
                }

                YearMonth current = YearMonth.now();
                int maxM = monthMap.values().stream().mapToInt(v -> v).max().orElse(1);
                monthlyBars = new ArrayList<>();
                for (Map.Entry<YearMonth, Integer> entry : monthMap.entrySet()) {
                    YearMonth ym = entry.getKey();
                    int val = entry.getValue();
                    String label = (ym.getYear() != current.getYear())
                            ? ym.getYear() + "." + ym.getMonthValue() + "월"
                            : ym.getMonthValue() + "월";
                    monthlyBars.add(SalesAnalysisResDto.SalesBar.builder()
                            .label(label).value(val)
                            .height(intRate(val, maxM)).best(val == maxM && maxM > 0).build());
                }
            }
            case "hourly" -> hourlySales = buildHourlySales(
                    orderingRepository.sumSalesByHour(storeId, rangeStart, rangeEnd));
            case "dow" -> dayOfWeekSales = buildDayOfWeekSales(
                    orderingRepository.sumSalesByDayOfWeek(storeId, rangeStart, rangeEnd));
        }

        // 매출 비교 (전월/전년) — 범위 기반
        int thisMonth = getMonthTotal(storeId, dto.getYear(), dto.getMonth());
        YearMonth prev = YearMonth.of(dto.getYear(), dto.getMonth()).minusMonths(1);
        int lastMonth = getMonthTotal(storeId, prev.getYear(), prev.getMonthValue());
        int lastYear = getMonthTotal(storeId, dto.getYear() - 1, dto.getMonth());

        return SalesAnalysisResDto.builder()
                .weeklyBars(weeklyBars).monthlyBars(monthlyBars)
                .hourlySales(hourlySales).dayOfWeekSales(dayOfWeekSales)
                .compare(SalesAnalysisResDto.SalesCompare.builder()
                        .thisMonth(thisMonth).lastMonth(lastMonth)
                        .momRate(growthRate(thisMonth, lastMonth))
                        .lastYear(lastYear).yoyRate(growthRate(thisMonth, lastYear)).build())
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  결제 분석
    // ═══════════════════════════════════════════════════════════

    public PaymentAnalysisResDto getPaymentAnalysis(Long storeId, PeriodReqDto dto) {
        LocalDateTime startAt = dto.getStartAt();
        LocalDateTime endAt = dto.getEndAt();
        YearMonth ym = YearMonth.of(dto.getYear(), dto.getMonth());

        List<Object[]> methodRaw = paymentRepository.sumByPaymentMethod(storeId, startAt, endAt);
        int totalAmt = 0, totalCnt = 0;
        int cC = 0, eC = 0, tC = 0, pC = 0;
        for (Object[] row : methodRaw) {
            String m = (String) row[0];
            int amt = ((Number) row[1]).intValue();
            int cnt = ((Number) row[2]).intValue();
            totalAmt += amt; totalCnt += cnt;
            switch (m != null ? m : "") {
                case "카드"   -> cC = cnt;
                case "간편결제" -> eC = cnt;
                case "계좌이체" -> tC = cnt;
                case "휴대폰"  -> pC = cnt;
            }
        }

        // 월 누적 총액
        int monthlyTotal;
        if ("monthly".equals(dto.getViewMode()) || (dto.getDay() == 0 && !"weekly".equals(dto.getViewMode()))) {
            monthlyTotal = totalAmt;
        } else {
            LocalDateTime ms = ym.atDay(1).atStartOfDay();
            LocalDateTime me = ym.plusMonths(1).atDay(1).atStartOfDay();
            monthlyTotal = paymentRepository.sumByPaymentMethod(storeId, ms, me)
                    .stream().mapToInt(r -> ((Number) r[1]).intValue()).sum();
        }

        // 결제 내역
        List<Payment> payments = paymentRepository.findRecentTransactions(storeId, startAt, endAt);
        Set<UUID> pGids = payments.stream().map(Payment::getGroupId).filter(Objects::nonNull)
                .map(g -> { try { return UUID.fromString(g); } catch (Exception e) { return null; } })
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, List<Ordering>> goMap = batchOrdersByGroupIds(pGids);
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        List<PaymentAnalysisResDto.TransactionDetail> txns = new ArrayList<>();
        for (Payment pay : payments) {
            List<PaymentAnalysisResDto.TransactionMenu> menus = new ArrayList<>();
            if (pay.getGroupId() != null) {
                try {
                    for (Ordering o : goMap.getOrDefault(UUID.fromString(pay.getGroupId()), List.of())) {
                        for (OrderingDetail d : o.getOrderDetail()) {
                            menus.add(PaymentAnalysisResDto.TransactionMenu.builder()
                                    .name(d.getMenu().getMenuName()).qty(d.getOrderingDetailQuantity())
                                    .price(d.getMenuPrice() * d.getOrderingDetailQuantity()).build());
                        }
                    }
                } catch (Exception ignored) {}
            }
            txns.add(PaymentAnalysisResDto.TransactionDetail.builder()
                    .id(pay.getId()).method(pay.getMethod() != null ? pay.getMethod() : "기타")
                    .time(pay.getApproveAt() != null ? pay.getApproveAt().format(timeFmt) : "")
                    .paymentKey(pay.getPaymentKey())
                    .amount(pay.getAmount()).tableNum(pay.getTableNum() != null ? pay.getTableNum() : 0)
                    .menus(menus).build());
        }

        return PaymentAnalysisResDto.builder()
                .methodBreakdown(PaymentAnalysisResDto.PaymentMethodBreakdown.builder()
                        .cardRate(intRate(cC, totalCnt)).easyPayRate(intRate(eC, totalCnt))
                        .transferRate(intRate(tC, totalCnt)).phoneRate(intRate(pC, totalCnt)).build())
                .summary(PaymentAnalysisResDto.PaymentSummary.builder()
                        .avgAmount(totalCnt > 0 ? totalAmt / totalCnt : 0)
                        .totalCount(totalCnt).totalAmount(totalAmt).monthlyTotal(monthlyTotal).build())
                .recentTransactions(txns).build();
    }

    // ═══════════════════════════════════════════════════════════
    //  테이블 분석
    // ═══════════════════════════════════════════════════════════

    public TableAnalysisResDto getTableAnalysis(Long storeId, PeriodReqDto dto) {
        LocalDateTime startAt = dto.getStartAt();
        LocalDateTime endAt = dto.getEndAt();
        YearMonth ym = YearMonth.of(dto.getYear(), dto.getMonth());
        LocalDate today = LocalDate.now();

        List<Object[]> salesRaw = orderingRepository.sumSalesByTable(storeId, startAt, endAt);
        int maxAmt = salesRaw.stream().mapToInt(r -> ((Number) r[1]).intValue()).max().orElse(1);
        List<TableAnalysisResDto.TableSales> tableSales = salesRaw.stream()
                .map(r -> TableAnalysisResDto.TableSales.builder()
                        .tableNum(((Number) r[0]).intValue()).amount(((Number) r[1]).intValue())
                        .count(((Number) r[2]).intValue())
                        .rate(intRate(((Number) r[1]).intValue(), maxAmt)).build())
                .collect(Collectors.toList());

        List<Object[]> turnRaw = orderingRepository.countGroupsByTable(storeId, startAt, endAt);
        int daysElapsed;
        if ("weekly".equals(dto.getViewMode())) {
            daysElapsed = dto.getWeekEnd().isAfter(today)
                    ? (int) java.time.temporal.ChronoUnit.DAYS.between(dto.getWeekStart(), today) + 1 : 7;
        } else if (dto.getDay() > 0) {
            daysElapsed = 1;
        } else {
            daysElapsed = today.getMonthValue() == dto.getMonth() && today.getYear() == dto.getYear()
                    ? today.getDayOfMonth() : ym.lengthOfMonth();
        }

        List<TableAnalysisResDto.TableTurnover> turnover = turnRaw.stream()
                .map(r -> {
                    int g = ((Number) r[1]).intValue();
                    double t = daysElapsed > 0 ? Math.round((double) g / daysElapsed * 10.0) / 10.0 : 0;
                    return TableAnalysisResDto.TableTurnover.builder()
                            .tableNum(((Number) r[0]).intValue()).turnover(t).build();
                }).collect(Collectors.toList());

        int useCount = orderingRepository.countDistinctGroupIds(storeId, startAt, endAt);
        double avgT = Math.round(turnover.stream()
                .mapToDouble(TableAnalysisResDto.TableTurnover::getTurnover).average().orElse(0) * 10.0) / 10.0;
        int totalOrders = tableSales.stream().mapToInt(TableAnalysisResDto.TableSales::getCount).sum();

        return TableAnalysisResDto.builder()
                .summary(TableAnalysisResDto.TableSummary.builder()
                        .avgTurnover(avgT).avgDuration(totalOrders > 0 ? 75 : 0).todayUseCount(useCount).build())
                .tableSales(tableSales).tableTurnover(turnover).build();
    }

    // ═══════════════════════════════════════════════════════════
    //  DTO 빌더 (Daily / Weekly / Monthly)
    // ═══════════════════════════════════════════════════════════

    private DailySettlementResDto buildDailyDto(PeriodReqDto dto, BasicAgg b, PaymentMethodAgg p,
                                                ProfitCalc pr, OrderAggregation a,
                                                List<DailySettlementResDto.OrderItem> orderItems,
                                                int prevDayTotal) {
        return DailySettlementResDto.builder()
                .date(LocalDate.of(dto.getYear(), dto.getMonth(), dto.getDay()))
                .totalAmount(b.totalAmount).orderCount(b.orderCount).averageOrderAmount(b.averageOrderAmount)
                .cancelCount(b.cancelCount).refundAmount(b.refundAmount).netSales(b.netSales).tableUseCount(b.tableUseCount)
                .prevDayTotal(prevDayTotal)
                .totalCost(pr.totalCost).grossProfit(pr.grossProfit).grossProfitRate(pr.grossProfitRate)
                .vat(pr.vat).netProfit(pr.netProfit).netProfitRate(pr.netProfitRate)
                .cardAmount(p.cardAmount).cardCount(p.cardCount)
                .transferAmount(p.transferAmount).transferCount(p.transferCount)
                .easyPayAmount(p.easyPayAmount).easyPayCount(p.easyPayCount)
                .phoneAmount(p.phoneAmount).phoneCount(p.phoneCount)
                .menuRankByCount(toDailyMenuRank(a, true)).menuRankByAmount(toDailyMenuRank(a, false))
                .categoryRank(toDailyCatRank(a)).tableStats(toDailyTableStats(a))
                .orders(orderItems).build();
    }

    private WeeklySettlementResDto buildWeeklyDto(LocalDate ws, LocalDate we, BasicAgg b, PaymentMethodAgg p,
                                                  ProfitCalc pr, OrderAggregation a,
                                                  List<WeeklySettlementResDto.DailyItem> daily,
                                                  List<WeeklySettlementResDto.WeekTrendItem> weeklyTrend) {
        return WeeklySettlementResDto.builder()
                .weekStart(ws).weekEnd(we)
                .totalAmount(b.totalAmount).orderCount(b.orderCount).averageOrderAmount(b.averageOrderAmount)
                .cancelCount(b.cancelCount).refundAmount(b.refundAmount).netSales(b.netSales).tableUseCount(b.tableUseCount)
                .totalCost(pr.totalCost).grossProfit(pr.grossProfit).grossProfitRate(pr.grossProfitRate)
                .vat(pr.vat).netProfit(pr.netProfit).netProfitRate(pr.netProfitRate)
                .cardAmount(p.cardAmount).cardCount(p.cardCount)
                .transferAmount(p.transferAmount).transferCount(p.transferCount)
                .easyPayAmount(p.easyPayAmount).easyPayCount(p.easyPayCount)
                .phoneAmount(p.phoneAmount).phoneCount(p.phoneCount)
                .dailyBreakdown(daily).weeklyTrend(weeklyTrend)
                .menuRankByCount(toWeeklyMenuRank(a, true)).menuRankByAmount(toWeeklyMenuRank(a, false))
                .categoryRank(toWeeklyCatRank(a)).tableStats(toWeeklyTableStats(a))
                .build();
    }

    private MonthlySettlementResDto buildMonthlyDto(PeriodReqDto dto, BasicAgg b, PaymentMethodAgg p,
                                                    ProfitCalc pr, OrderAggregation a,
                                                    Map<Integer, Integer> dailySales,
                                                    Map<Integer, Integer> dailyNetProfit,
                                                    int dailyAvg,
                                                    List<MonthlySettlementResDto.WeeklyItem> wb,
                                                    List<MonthlySettlementResDto.MonthTrendItem> monthlyTrend) {
        return MonthlySettlementResDto.builder()
                .year(dto.getYear()).month(dto.getMonth())
                .totalAmount(b.totalAmount).orderCount(b.orderCount).averageOrderAmount(b.averageOrderAmount)
                .dailyAverageSales(dailyAvg)
                .cancelCount(b.cancelCount).refundAmount(b.refundAmount).netSales(b.netSales).tableUseCount(b.tableUseCount)
                .totalCost(pr.totalCost).grossProfit(pr.grossProfit).grossProfitRate(pr.grossProfitRate)
                .vat(pr.vat).netProfit(pr.netProfit).netProfitRate(pr.netProfitRate)
                .cardAmount(p.cardAmount).cardCount(p.cardCount)
                .transferAmount(p.transferAmount).transferCount(p.transferCount)
                .easyPayAmount(p.easyPayAmount).easyPayCount(p.easyPayCount)
                .phoneAmount(p.phoneAmount).phoneCount(p.phoneCount)
                .dailySales(dailySales).dailyNetProfit(dailyNetProfit)
                .weeklyBreakdown(wb).monthlyTrend(monthlyTrend)
                .menuRankByCount(toMonthlyMenuRank(a, true)).menuRankByAmount(toMonthlyMenuRank(a, false))
                .categoryRank(toMonthlyCatRank(a)).tableStats(toMonthlyTableStats(a))
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  Agg → MenuRankItem 변환 (DTO 타입별)
    // ═══════════════════════════════════════════════════════════

    private List<DailySettlementResDto.MenuRankItem> toDailyMenuRank(OrderAggregation a, boolean byCount) {
        int idx = byCount ? 0 : 1;
        List<Map.Entry<String, int[]>> sorted = new ArrayList<>(a.menuMap.entrySet());
        sorted.sort((x, y) -> Integer.compare(y.getValue()[idx], x.getValue()[idx]));
        List<DailySettlementResDto.MenuRankItem> list = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            var e = sorted.get(i);
            list.add(DailySettlementResDto.MenuRankItem.builder()
                    .rank(i + 1).menuName(e.getKey()).categoryName(a.menuCategoryMap.get(e.getKey()))
                    .salesCount(e.getValue()[0]).salesAmount(e.getValue()[1])
                    .cost(e.getValue()[2]).profit(e.getValue()[1] - e.getValue()[2]).build());
        }
        return list;
    }

    private List<WeeklySettlementResDto.MenuRankItem> toWeeklyMenuRank(OrderAggregation a, boolean byCount) {
        int idx = byCount ? 0 : 1;
        List<Map.Entry<String, int[]>> sorted = new ArrayList<>(a.menuMap.entrySet());
        sorted.sort((x, y) -> Integer.compare(y.getValue()[idx], x.getValue()[idx]));
        List<WeeklySettlementResDto.MenuRankItem> list = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            var e = sorted.get(i);
            list.add(WeeklySettlementResDto.MenuRankItem.builder()
                    .rank(i + 1).menuName(e.getKey()).categoryName(a.menuCategoryMap.get(e.getKey()))
                    .salesCount(e.getValue()[0]).salesAmount(e.getValue()[1])
                    .cost(e.getValue()[2]).profit(e.getValue()[1] - e.getValue()[2]).build());
        }
        return list;
    }

    private List<MonthlySettlementResDto.MenuRankItem> toMonthlyMenuRank(OrderAggregation a, boolean byCount) {
        int idx = byCount ? 0 : 1;
        List<Map.Entry<String, int[]>> sorted = new ArrayList<>(a.menuMap.entrySet());
        sorted.sort((x, y) -> Integer.compare(y.getValue()[idx], x.getValue()[idx]));
        List<MonthlySettlementResDto.MenuRankItem> list = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            var e = sorted.get(i);
            list.add(MonthlySettlementResDto.MenuRankItem.builder()
                    .rank(i + 1).menuName(e.getKey()).categoryName(a.menuCategoryMap.get(e.getKey()))
                    .salesCount(e.getValue()[0]).salesAmount(e.getValue()[1])
                    .cost(e.getValue()[2]).profit(e.getValue()[1] - e.getValue()[2]).build());
        }
        return list;
    }

    // ═══════════════════════════════════════════════════════════
    //  Agg → CategoryRankItem / TableStatItem 변환
    // ═══════════════════════════════════════════════════════════

    private List<DailySettlementResDto.CategoryRankItem> toDailyCatRank(OrderAggregation a) {
        List<Map.Entry<String, int[]>> sorted = new ArrayList<>(a.catMap.entrySet());
        sorted.sort((x, y) -> Integer.compare(y.getValue()[1], x.getValue()[1]));
        List<DailySettlementResDto.CategoryRankItem> list = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            var e = sorted.get(i);
            list.add(DailySettlementResDto.CategoryRankItem.builder()
                    .rank(i + 1).categoryName(e.getKey())
                    .salesCount(e.getValue()[0]).salesAmount(e.getValue()[1]).build());
        }
        return list;
    }

    private List<WeeklySettlementResDto.CategoryRankItem> toWeeklyCatRank(OrderAggregation a) {
        List<Map.Entry<String, int[]>> sorted = new ArrayList<>(a.catMap.entrySet());
        sorted.sort((x, y) -> Integer.compare(y.getValue()[1], x.getValue()[1]));
        List<WeeklySettlementResDto.CategoryRankItem> list = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            var e = sorted.get(i);
            list.add(WeeklySettlementResDto.CategoryRankItem.builder()
                    .rank(i + 1).categoryName(e.getKey())
                    .salesCount(e.getValue()[0]).salesAmount(e.getValue()[1]).build());
        }
        return list;
    }

    private List<MonthlySettlementResDto.CategoryRankItem> toMonthlyCatRank(OrderAggregation a) {
        List<Map.Entry<String, int[]>> sorted = new ArrayList<>(a.catMap.entrySet());
        sorted.sort((x, y) -> Integer.compare(y.getValue()[1], x.getValue()[1]));
        List<MonthlySettlementResDto.CategoryRankItem> list = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            var e = sorted.get(i);
            list.add(MonthlySettlementResDto.CategoryRankItem.builder()
                    .rank(i + 1).categoryName(e.getKey())
                    .salesCount(e.getValue()[0]).salesAmount(e.getValue()[1]).build());
        }
        return list;
    }

    private List<DailySettlementResDto.TableStatItem> toDailyTableStats(OrderAggregation a) {
        return a.tableMap.entrySet().stream()
                .sorted((x, y) -> Integer.compare(y.getValue()[1], x.getValue()[1]))
                .map(e -> DailySettlementResDto.TableStatItem.builder()
                        .tableNum(e.getKey()).useCount(e.getValue()[0])
                        .salesAmount(e.getValue()[1]).orderCount(e.getValue()[2]).build())
                .collect(Collectors.toList());
    }

    private List<WeeklySettlementResDto.TableStatItem> toWeeklyTableStats(OrderAggregation a) {
        return a.tableMap.entrySet().stream()
                .sorted((x, y) -> Integer.compare(y.getValue()[1], x.getValue()[1]))
                .map(e -> WeeklySettlementResDto.TableStatItem.builder()
                        .tableNum(e.getKey()).useCount(e.getValue()[0])
                        .salesAmount(e.getValue()[1]).orderCount(e.getValue()[2]).build())
                .collect(Collectors.toList());
    }

    private List<MonthlySettlementResDto.TableStatItem> toMonthlyTableStats(OrderAggregation a) {
        return a.tableMap.entrySet().stream()
                .sorted((x, y) -> Integer.compare(y.getValue()[1], x.getValue()[1]))
                .map(e -> MonthlySettlementResDto.TableStatItem.builder()
                        .tableNum(e.getKey()).useCount(e.getValue()[0])
                        .salesAmount(e.getValue()[1]).orderCount(e.getValue()[2]).build())
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════
    //  매출 분석 차트 헬퍼
    // ═══════════════════════════════════════════════════════════

    private List<SalesAnalysisResDto.HourlySales> buildHourlySales(List<Object[]> raw) {
        Map<Integer, Integer> map = new LinkedHashMap<>();
        for (Object[] r : raw) map.put(((Number) r[0]).intValue(), ((Number) r[1]).intValue());
        int max = map.values().stream().mapToInt(v -> v).max().orElse(1);
        List<SalesAnalysisResDto.HourlySales> list = new ArrayList<>(24);
        for (int h = 0; h < 24; h++) {
            int v = map.getOrDefault(h, 0);
            list.add(SalesAnalysisResDto.HourlySales.builder().hour(h).amount(v).height(intRate(v, max)).build());
        }
        return list;
    }

    private List<SalesAnalysisResDto.SalesBar> buildDayOfWeekSales(List<Object[]> raw) {
        String[] labels = {"일", "월", "화", "수", "목", "금", "토"};
        int[] vals = new int[7];
        for (Object[] r : raw) vals[((Number) r[0]).intValue() - 1] = ((Number) r[1]).intValue();
        int max = Arrays.stream(vals).max().orElse(1);
        List<SalesAnalysisResDto.SalesBar> list = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            list.add(SalesAnalysisResDto.SalesBar.builder()
                    .label(labels[i]).value(vals[i]).height(intRate(vals[i], max))
                    .best(vals[i] == max && max > 0).build());
        }
        return list;
    }

    /** 일별 Raw → 주차별 합산 */
    private List<MonthlySettlementResDto.WeeklyItem> buildWeeklyBreakdown(List<Object[]> dailyRaw) {
        Map<Integer, int[]> weekMap = new LinkedHashMap<>();
        Map<Integer, LocalDate[]> weekDates = new LinkedHashMap<>();
        for (Object[] row : dailyRaw) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            int revenue = ((Number) row[1]).intValue();
            int orderCnt = ((Number) row[2]).intValue();
            int weekNum = (date.getDayOfMonth() - 1) / 7 + 1;
            LocalDate ws = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            weekMap.computeIfAbsent(weekNum, k -> new int[]{0, 0});
            weekMap.get(weekNum)[0] += revenue;
            weekMap.get(weekNum)[1] += orderCnt;
            weekDates.computeIfAbsent(weekNum, k -> new LocalDate[]{ws, ws.plusDays(6)});
        }
        return weekMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> MonthlySettlementResDto.WeeklyItem.builder()
                        .week(e.getKey()).weekStart(weekDates.get(e.getKey())[0])
                        .weekEnd(weekDates.get(e.getKey())[1])
                        .totalAmount(e.getValue()[0]).orderCount(e.getValue()[1]).build())
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════
    //  트렌드 빌더
    // ═══════════════════════════════════════════════════════════

    /** 최근 5주 트렌드 (현재 주 포함, 과거 4주) — 단일 쿼리로 집계 */
    private List<WeeklySettlementResDto.WeekTrendItem> buildWeeklyTrend(Long storeId, LocalDate currentWeekStart) {
        LocalDate firstWeekStart = currentWeekStart.minusWeeks(4);
        LocalDateTime startAt = firstWeekStart.atStartOfDay();
        LocalDateTime endAt = currentWeekStart.plusWeeks(1).atStartOfDay();

        List<Object[]> raw = orderingRepository.sumSalesByDate(storeId, startAt, endAt);
        Map<LocalDate, int[]> weekMap = new LinkedHashMap<>();
        for (int i = 0; i < 5; i++) weekMap.put(firstWeekStart.plusWeeks(i), new int[]{0, 0, 0});

        for (Object[] r : raw) {
            LocalDate date = ((java.sql.Date) r[0]).toLocalDate();
            LocalDate weekMon = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            int[] v = weekMap.get(weekMon);
            if (v != null) {
                v[0] += ((Number) r[1]).intValue(); // totalAmount
                v[1] += ((Number) r[2]).intValue(); // orderCount
                v[2] += ((Number) r[3]).intValue(); // cancelCount
            }
        }

        List<WeeklySettlementResDto.WeekTrendItem> trend = new ArrayList<>(5);
        for (Map.Entry<LocalDate, int[]> entry : weekMap.entrySet()) {
            LocalDate ws = entry.getKey();
            LocalDate we = ws.plusDays(6);
            int totalAmt = entry.getValue()[0];
            int orderCnt = entry.getValue()[1];
            int cancelCnt = entry.getValue()[2];
            String label = ws.getMonthValue() + "/" + ws.getDayOfMonth()
                    + "~" + we.getMonthValue() + "/" + we.getDayOfMonth();
            trend.add(WeeklySettlementResDto.WeekTrendItem.builder()
                    .weekStart(ws).weekEnd(we).label(label)
                    .totalAmount(totalAmt).orderCount(orderCnt).cancelCount(cancelCnt)
                    .averageOrderAmount(orderCnt > 0 ? totalAmt / orderCnt : 0).build());
        }
        return trend;
    }

    /** 최근 12개월 트렌드 (이번달 포함, 과거 11개월) — 단일 쿼리로 집계 */
    private List<MonthlySettlementResDto.MonthTrendItem> buildMonthlyTrend(Long storeId, int year, int month) {
        YearMonth current = YearMonth.of(year, month);
        YearMonth start = current.minusMonths(11);
        LocalDateTime startAt = start.atDay(1).atStartOfDay();
        LocalDateTime endAt = current.plusMonths(1).atDay(1).atStartOfDay();

        // 단일 쿼리로 일별 데이터 → 월별 집계
        List<Object[]> raw = orderingRepository.sumSalesByDate(storeId, startAt, endAt);
        Map<YearMonth, int[]> monthMap = new LinkedHashMap<>();
        for (int i = 0; i < 12; i++) monthMap.put(start.plusMonths(i), new int[]{0, 0});

        for (Object[] r : raw) {
            LocalDate date = ((java.sql.Date) r[0]).toLocalDate();
            YearMonth ym = YearMonth.from(date);
            int[] v = monthMap.get(ym);
            if (v != null) {
                v[0] += ((Number) r[1]).intValue(); // totalAmount
                v[1] += ((Number) r[2]).intValue(); // orderCount
            }
        }

        List<MonthlySettlementResDto.MonthTrendItem> trend = new ArrayList<>(12);
        for (Map.Entry<YearMonth, int[]> entry : monthMap.entrySet()) {
            YearMonth ym = entry.getKey();
            int totalAmt = entry.getValue()[0];
            int orderCnt = entry.getValue()[1];
            trend.add(MonthlySettlementResDto.MonthTrendItem.builder()
                    .year(ym.getYear()).month(ym.getMonthValue())
                    .label(ym.getYear() + "." + String.format("%02d", ym.getMonthValue()))
                    .totalAmount(totalAmt).orderCount(orderCnt)
                    .averageOrderAmount(orderCnt > 0 ? totalAmt / orderCnt : 0).build());
        }
        return trend;
    }

    // ═══════════════════════════════════════════════════════════
    //  유틸
    // ═══════════════════════════════════════════════════════════

    private static int intRate(int value, int total) {
        return total > 0 ? (int) ((long) value * 100 / total) : 0;
    }

    private static double pctRate(int value, int total) {
        return total > 0 ? Math.round((double) value / total * 1000.0) / 10.0 : 0;
    }

    private static double growthRate(int current, int previous) {
        return previous > 0 ? Math.round((double) (current - previous) / previous * 1000.0) / 10.0 : 0;
    }

    private List<MenuAnalysisResDto.MenuCombo> buildCombos(Map<UUID, Set<String>> groupMenus) {
        Map<String, Integer> comboMap = new HashMap<>();
        for (Set<String> menus : groupMenus.values()) {
            List<String> sorted = new ArrayList<>(menus);
            Collections.sort(sorted);
            for (int i = 0; i < sorted.size(); i++)
                for (int j = i + 1; j < sorted.size(); j++)
                    comboMap.merge(sorted.get(i) + " + " + sorted.get(j), 1, Integer::sum);
        }
        return comboMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(10)
                .map(e -> MenuAnalysisResDto.MenuCombo.builder().pair(e.getKey()).count(e.getValue()).build())
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════
    //  재료 정산 요약
    // ═══════════════════════════════════════════════════════════

    public IngredientSummaryResDto getIngredientSummary(Long storeId, PeriodReqDto dto) {
        LocalDateTime startAt = dto.getMonthStart().atStartOfDay();
        LocalDateTime endAt = dto.getMonthEnd().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        // 이번 달 입고 총액
        int monthlyIntakeCost = ingredientDetailRepository.sumTotalPriceByStoreAndPeriod(storeId, startAt, endAt);

        // ★ 로스 — IngredientLoss 테이블에서 조회 (하드딜리트 전에 기록됨)
        List<IngredientLoss> lossRecords = ingredientLossRepository.findByStoreAndPeriod(storeId, startAt, endAt);
        List<IngredientSummaryResDto.LossItem> lossItems = new ArrayList<>();
        int lossCost = 0;
        for (IngredientLoss l : lossRecords) {
            lossCost += l.getLossAmount();
            lossItems.add(IngredientSummaryResDto.LossItem.builder()
                    .ingredientName(l.getIngredientName())
                    .remainQuantity(l.getLostQuantity())
                    .unitPrice(l.getUnitPrice())
                    .lossAmount(l.getLossAmount())
                    .deadline(l.getDeadline())
                    .build());
        }

        // 로스율 — 이번 달 입고 대비
        int base = monthlyIntakeCost > 0 ? monthlyIntakeCost
                : ingredientDetailRepository.sumAllTotalPriceByStore(storeId);
        double lossRate = base > 0 ? Math.round(lossCost * 1000.0 / base) / 10.0 : 0;

        // 유통기한 임박 (3일 이내) — 아직 살아있는 IngredientDetail에서 조회
        LocalDateTime threshold = now.plusDays(3);
        List<IngredientDetail> soonItems = ingredientDetailRepository.findExpiringSoon(storeId, now, threshold);
        List<IngredientSummaryResDto.ExpiringSoonItem> expiringSoonItems = new ArrayList<>();
        for (IngredientDetail d : soonItems) {
            long daysLeft = java.time.Duration.between(now, d.getDeadline()).toDays();
            expiringSoonItems.add(IngredientSummaryResDto.ExpiringSoonItem.builder()
                    .ingredientName(d.getIngredient().getName())
                    .remainQuantity(d.getCurrentQuantity())
                    .unitPrice(d.getUnitPrice())
                    .valueAtRisk(d.getUnitPrice() * d.getCurrentQuantity())
                    .deadline(d.getDeadline())
                    .daysLeft((int) Math.max(daysLeft, 0))
                    .build());
        }

        return IngredientSummaryResDto.builder()
                .monthlyIntakeCost(monthlyIntakeCost)
                .lossCost(lossCost)
                .lossRate(lossRate)
                .expiringSoonCount(soonItems.size())
                .lossItems(lossItems)
                .expiringSoonItems(expiringSoonItems)
                .build();
    }
}