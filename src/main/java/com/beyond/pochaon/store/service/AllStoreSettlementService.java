package com.beyond.pochaon.store.service;

import com.beyond.pochaon.ingredient.repository.IngredientDetailRepository;
import com.beyond.pochaon.ordering.repository.OrderingRepository;
import com.beyond.pochaon.owner.domain.Owner;
import com.beyond.pochaon.owner.repository.OwnerRepository;
import com.beyond.pochaon.store.domain.Store;
import com.beyond.pochaon.store.dto.*;
import com.beyond.pochaon.store.common.ProfitCalculator;
import com.beyond.pochaon.store.repository.StoreRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Slf4j
public class AllStoreSettlementService {

    private final OwnerRepository ownerRepository;
    private final StoreRepository storeRepository;
    private final OrderingRepository orderingRepository;
    private final IngredientDetailRepository ingredientDetailRepository;

    @Autowired
    public AllStoreSettlementService(OwnerRepository ownerRepository,
                                     StoreRepository storeRepository,
                                     OrderingRepository orderingRepository,
                                     IngredientDetailRepository ingredientDetailRepository) {
        this.ownerRepository = ownerRepository;
        this.storeRepository = storeRepository;
        this.orderingRepository = orderingRepository;
        this.ingredientDetailRepository = ingredientDetailRepository;
    }

    // ═══════════════════════════════════════════════════════════
    //  전체 매장 요약 (배치 쿼리 — 매장 수 무관하게 ~8 쿼리)
    // ═══════════════════════════════════════════════════════════

    public AllStoreSummaryResDto getAllStoreSummary(String email, PeriodReqDto dto) {
        List<Store> stores = getOwnerStores(email);
        List<Long> storeIds = toIds(stores);

        LocalDateTime startAt = dto.getStartAt();
        LocalDateTime endAt = dto.getEndAt();

        int totalRevenue = orderingRepository.sumTotalRevenueByStores(storeIds, startAt, endAt);
        int totalOrders = orderingRepository.countCompletedOrdersByStores(storeIds, startAt, endAt);
        int totalCancels = orderingRepository.countCancelledOrdersByStores(storeIds, startAt, endAt);
        int totalIngredientCost = ingredientDetailRepository.sumTotalPriceByStores(storeIds, startAt, endAt);

        Map<Integer, Integer> dailySales = toDayMap(
                orderingRepository.sumSalesByDateForStores(storeIds, startAt, endAt));
        Map<Integer, Integer> dailyCost = toDayMap(
                ingredientDetailRepository.sumDailyCostByStores(storeIds, startAt, endAt));

        Map<Integer, Integer> dailyNetProfit = new HashMap<>();
        for (Map.Entry<Integer, Integer> e : dailySales.entrySet()) {
            dailyNetProfit.put(e.getKey(),
                    ProfitCalculator.netProfit(e.getValue(), dailyCost.getOrDefault(e.getKey(), 0)));
        }

        int totalNetProfit = ProfitCalculator.netProfit(totalRevenue, totalIngredientCost);
        int avgOrderAmount = totalOrders > 0 ? totalRevenue / totalOrders : 0;

        // 성장률
        YearMonth currentYm = YearMonth.of(dto.getYear(), dto.getMonth());
        YearMonth prevYm = currentYm.minusMonths(1);
        LocalDateTime monthStart = currentYm.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentYm.plusMonths(1).atDay(1).atStartOfDay();
        LocalDateTime prevStart = prevYm.atDay(1).atStartOfDay();
        LocalDateTime prevEnd = prevYm.plusMonths(1).atDay(1).atStartOfDay();

        boolean isSameMonth = startAt.equals(monthStart) && endAt.equals(monthEnd);
        int thisMonthRev = isSameMonth ? totalRevenue
                : orderingRepository.sumTotalRevenueByStores(storeIds, monthStart, monthEnd);
        int thisMonthCost = isSameMonth ? totalIngredientCost
                : ingredientDetailRepository.sumTotalPriceByStores(storeIds, monthStart, monthEnd);
        int thisMonthProfit = ProfitCalculator.netProfit(thisMonthRev, thisMonthCost);

        int lastMonthRev = orderingRepository.sumTotalRevenueByStores(storeIds, prevStart, prevEnd);
        int lastMonthCost = ingredientDetailRepository.sumTotalPriceByStores(storeIds, prevStart, prevEnd);
        int lastMonthProfit = ProfitCalculator.netProfit(lastMonthRev, lastMonthCost);

        double monthGrowth = lastMonthProfit != 0
                ? Math.round((double)(thisMonthProfit - lastMonthProfit) / Math.abs(lastMonthProfit) * 1000.0) / 10.0
                : 0;

        return AllStoreSummaryResDto.builder()
                .totalRevenue(totalRevenue).totalNetProfit(totalNetProfit)
                .totalOrders(totalOrders).totalCancels(totalCancels)
                .storeCount(stores.size()).monthGrowthRate(monthGrowth)
                .avgOrderAmount(avgOrderAmount)
                .dailySales(dailySales).dailyNetProfit(dailyNetProfit)
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  일별 상세 (달력 날짜 클릭 모달)
    // ═══════════════════════════════════════════════════════════

    public AllStoreDayResDto getAllStoreDay(String email, PeriodReqDto dto) {
        List<Store> stores = getOwnerStores(email);
        List<Long> storeIds = toIds(stores);
        Map<Long, String> nameMap = toNameMap(stores);

        LocalDate targetDate = LocalDate.of(dto.getYear(), dto.getMonth(), dto.getDay());
        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime dayEnd = targetDate.plusDays(1).atStartOfDay();

        // 합산
        int totalRevenue = orderingRepository.sumTotalRevenueByStores(storeIds, dayStart, dayEnd);
        int totalOrders = orderingRepository.countCompletedOrdersByStores(storeIds, dayStart, dayEnd);
        int totalCancels = orderingRepository.countCancelledOrdersByStores(storeIds, dayStart, dayEnd);
        int avgOrderAmount = totalOrders > 0 ? totalRevenue / totalOrders : 0;

        // ★ 재료비 → 순이익
        int dayCost = ingredientDetailRepository.sumTotalPriceByStores(storeIds, dayStart, dayEnd);
        int totalNetProfit = ProfitCalculator.netProfit(totalRevenue, dayCost);

        // 매장별 당일·월·전일 매출
        Map<Long, int[]> perStore = toStoreRevenueMap(
                orderingRepository.sumRevenuePerStore(storeIds, dayStart, dayEnd));
        LocalDateTime monthStart = targetDate.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = targetDate.plusMonths(1).withDayOfMonth(1).atStartOfDay();
        Map<Long, int[]> monthPerStore = toStoreRevenueMap(
                orderingRepository.sumRevenuePerStore(storeIds, monthStart, monthEnd));
        int daysInMonth = targetDate.lengthOfMonth();

        LocalDateTime ydayStart = targetDate.minusDays(1).atStartOfDay();
        Map<Long, int[]> ydayPerStore = toStoreRevenueMap(
                orderingRepository.sumRevenuePerStore(storeIds, ydayStart, dayStart));

        // 매장별 조립
        List<AllStoreDayResDto.StoreDayItem> ranking = new ArrayList<>();
        List<AllStoreDayResDto.StoreGrowthItem> growth = new ArrayList<>();
        List<AllStoreDayResDto.StoreDayItem> avgAmountList = new ArrayList<>();

        for (Store store : stores) {
            Long sid = store.getId();
            int[] cur = perStore.getOrDefault(sid, new int[]{0, 0});
            int rev = cur[0], cnt = cur[1];
            int avg = cnt > 0 ? rev / cnt : 0;
            int avgDaily = daysInMonth > 0 ? monthPerStore.getOrDefault(sid, new int[]{0, 0})[0] / daysInMonth : 0;

            ranking.add(AllStoreDayResDto.StoreDayItem.builder()
                    .storeId(sid).storeName(nameMap.get(sid))
                    .revenue(rev).orderCount(cnt).avgAmount(avg).avgDailyRevenue(avgDaily).build());

            int ydayRev = ydayPerStore.getOrDefault(sid, new int[]{0, 0})[0];
            growth.add(AllStoreDayResDto.StoreGrowthItem.builder()
                    .storeId(sid).storeName(nameMap.get(sid))
                    .today(rev).yesterday(ydayRev).growthRate(growthRate(rev, ydayRev)).build());

            if (cnt > 0) {
                avgAmountList.add(AllStoreDayResDto.StoreDayItem.builder()
                        .storeId(sid).storeName(nameMap.get(sid))
                        .revenue(rev).orderCount(cnt).avgAmount(avg).build());
            }
        }

        ranking.sort((a, b) -> b.getRevenue() - a.getRevenue());
        growth.sort((a, b) -> Double.compare(b.getGrowthRate(), a.getGrowthRate()));
        avgAmountList.sort((a, b) -> b.getAvgAmount() - a.getAvgAmount());

        // 매출 비교
        int yesterdayRevenue = orderingRepository.sumTotalRevenueByStores(storeIds, ydayStart, dayStart);
        int lastWeekRevenue = orderingRepository.sumTotalRevenueByStores(storeIds,
                targetDate.minusWeeks(1).atStartOfDay(), targetDate.minusWeeks(1).plusDays(1).atStartOfDay());
        int lastMonthRevenue = orderingRepository.sumTotalRevenueByStores(storeIds,
                targetDate.minusMonths(1).atStartOfDay(), targetDate.minusMonths(1).plusDays(1).atStartOfDay());

        return AllStoreDayResDto.builder()
                .totalRevenue(totalRevenue).totalNetProfit(totalNetProfit)
                .totalOrders(totalOrders).totalCancels(totalCancels)
                .avgOrderAmount(avgOrderAmount).storeCount(stores.size())
                .todayRevenue(totalRevenue).todayNetProfit(totalNetProfit)
                .yesterdayRevenue(yesterdayRevenue)
                .lastWeekRevenue(lastWeekRevenue).lastMonthRevenue(lastMonthRevenue)
                .storeRanking(ranking).storeGrowth(growth).storeAvgAmount(avgAmountList)
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  기간별 매출비교 (전체 매장 합산)
    // ═══════════════════════════════════════════════════════════

    public SalesAnalysisResDto getAllStoreSalesAnalysis(String email, PeriodReqDto dto) {
        List<Long> storeIds = toIds(getOwnerStores(email));
        String period = dto.getPeriod() != null ? dto.getPeriod() : "weekly";
        LocalDateTime rangeStart = dto.getStartAt();
        LocalDateTime rangeEnd = dto.getEndAt();

        List<SalesAnalysisResDto.SalesBar> weeklyBars = null, monthlyBars = null, dayOfWeekSales = null;
        List<SalesAnalysisResDto.HourlySales> hourlySales = null;

        switch (period) {
            case "weekly" -> weeklyBars = buildWeeklyBars(
                    orderingRepository.sumSalesByDateForStores(storeIds, rangeStart, rangeEnd), rangeStart, rangeEnd);
            case "monthly" -> monthlyBars = buildMonthlyBars(
                    orderingRepository.sumSalesByDateForStores(storeIds, rangeStart, rangeEnd), rangeStart, rangeEnd);
            case "hourly" -> hourlySales = buildHourlySales(
                    orderingRepository.sumSalesByHourForStores(storeIds, rangeStart, rangeEnd));
            case "dow" -> dayOfWeekSales = buildDayOfWeekSales(
                    orderingRepository.sumSalesByDayOfWeekForStores(storeIds, rangeStart, rangeEnd));
        }

        int thisMonth = getAllStoreMonthTotal(storeIds, dto.getYear(), dto.getMonth());
        YearMonth prev = YearMonth.of(dto.getYear(), dto.getMonth()).minusMonths(1);
        int lastMonth = getAllStoreMonthTotal(storeIds, prev.getYear(), prev.getMonthValue());
        int lastYear = getAllStoreMonthTotal(storeIds, dto.getYear() - 1, dto.getMonth());

        return SalesAnalysisResDto.builder()
                .weeklyBars(weeklyBars).monthlyBars(monthlyBars)
                .hourlySales(hourlySales).dayOfWeekSales(dayOfWeekSales)
                .compare(SalesAnalysisResDto.SalesCompare.builder()
                        .thisMonth(thisMonth).lastMonth(lastMonth).momRate(growthRate(thisMonth, lastMonth))
                        .lastYear(lastYear).yoyRate(growthRate(thisMonth, lastYear)).build())
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  매장별 비교 (배치 쿼리 — 3 쿼리)
    // ═══════════════════════════════════════════════════════════

    public AllStoreCompareResDto getStoreComparison(String email, PeriodReqDto dto) {
        List<Store> stores = getOwnerStores(email);
        List<Long> storeIds = toIds(stores);
        Map<Long, String> nameMap = toNameMap(stores);

        LocalDateTime startAt = dto.getStartAt();
        LocalDateTime endAt = dto.getEndAt();

        YearMonth currentYm = YearMonth.of(dto.getYear(), dto.getMonth());
        YearMonth prevYm = currentYm.minusMonths(1);
        LocalDateTime monthStart = currentYm.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentYm.plusMonths(1).atDay(1).atStartOfDay();
        LocalDateTime prevStart = prevYm.atDay(1).atStartOfDay();
        LocalDateTime prevEnd = prevYm.plusMonths(1).atDay(1).atStartOfDay();

        Map<Long, int[]> currentMap = toStoreRevenueMap(
                orderingRepository.sumRevenuePerStore(storeIds, startAt, endAt));
        Map<Long, int[]> thisMonthMap = toStoreRevenueMap(
                orderingRepository.sumRevenuePerStore(storeIds, monthStart, monthEnd));
        Map<Long, int[]> lastMonthMap = toStoreRevenueMap(
                orderingRepository.sumRevenuePerStore(storeIds, prevStart, prevEnd));

        List<AllStoreCompareResDto.StoreCompareItem> items = new ArrayList<>();
        for (Store store : stores) {
            Long sid = store.getId();
            int[] cur = currentMap.getOrDefault(sid, new int[]{0, 0});
            int revenue = cur[0], orderCount = cur[1];
            int avgAmount = orderCount > 0 ? revenue / orderCount : 0;
            int thisMonth = thisMonthMap.getOrDefault(sid, new int[]{0, 0})[0];
            int lastMonth = lastMonthMap.getOrDefault(sid, new int[]{0, 0})[0];
            items.add(AllStoreCompareResDto.StoreCompareItem.builder()
                    .storeId(sid).storeName(nameMap.get(sid))
                    .revenue(revenue).orderCount(orderCount).avgAmount(avgAmount)
                    .thisMonth(thisMonth).lastMonth(lastMonth).growthRate(growthRate(thisMonth, lastMonth))
                    .build());
        }
        return AllStoreCompareResDto.builder().stores(items).build();
    }

    // ═══════════════════════════════════════════════════════════
    //  내부 헬퍼
    // ═══════════════════════════════════════════════════════════

    private List<Store> getOwnerStores(String email) {
        Owner owner = ownerRepository.findByOwnerEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Owner not found: " + email));
        return storeRepository.findByOwnerId(owner.getId());
    }

    private List<Long> toIds(List<Store> stores) {
        return stores.stream().map(Store::getId).collect(Collectors.toList());
    }

    private Map<Long, String> toNameMap(List<Store> stores) {
        return stores.stream().collect(Collectors.toMap(Store::getId, Store::getStoreName));
    }

    private Map<Integer, Integer> toDayMap(List<Object[]> rows) {
        Map<Integer, Integer> map = new HashMap<>();
        for (Object[] row : rows) {
            map.merge(((java.sql.Date) row[0]).toLocalDate().getDayOfMonth(),
                    ((Number) row[1]).intValue(), Integer::sum);
        }
        return map;
    }

    private Map<Long, int[]> toStoreRevenueMap(List<Object[]> rows) {
        Map<Long, int[]> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put(((Number) row[0]).longValue(),
                    new int[]{((Number) row[1]).intValue(), ((Number) row[2]).intValue()});
        }
        return map;
    }

    private int getAllStoreMonthTotal(List<Long> storeIds, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        return orderingRepository.sumTotalRevenueByStores(storeIds,
                ym.atDay(1).atStartOfDay(), ym.plusMonths(1).atDay(1).atStartOfDay());
    }

    // ── 차트 빌더 ──

    private List<SalesAnalysisResDto.SalesBar> buildWeeklyBars(List<Object[]> raw,
                                                               LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        Map<LocalDate, Integer> weekMap = new LinkedHashMap<>();
        LocalDate firstMon = rangeStart.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lastMon = rangeEnd.toLocalDate().minusDays(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        for (LocalDate m = firstMon; !m.isAfter(lastMon); m = m.plusWeeks(1)) weekMap.put(m, 0);
        for (Object[] r : raw) {
            LocalDate d = ((java.sql.Date) r[0]).toLocalDate();
            weekMap.merge(d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    ((Number) r[1]).intValue(), Integer::sum);
        }
        int max = weekMap.values().stream().mapToInt(v -> v).max().orElse(1);
        List<SalesAnalysisResDto.SalesBar> bars = new ArrayList<>();
        for (var e : weekMap.entrySet()) {
            LocalDate ws = e.getKey(), we = ws.plusDays(6);
            int val = e.getValue();
            bars.add(SalesAnalysisResDto.SalesBar.builder()
                    .label(ws.getMonthValue() + "/" + ws.getDayOfMonth() + "~" + we.getMonthValue() + "/" + we.getDayOfMonth())
                    .value(val).height(intRate(val, max)).best(val == max && max > 0).build());
        }
        return bars;
    }

    private List<SalesAnalysisResDto.SalesBar> buildMonthlyBars(List<Object[]> raw,
                                                                LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        Map<YearMonth, Integer> monthMap = new LinkedHashMap<>();
        YearMonth startYm = YearMonth.from(rangeStart.toLocalDate());
        YearMonth endYm = YearMonth.from(rangeEnd.toLocalDate().minusDays(1));
        for (YearMonth ym = startYm; !ym.isAfter(endYm); ym = ym.plusMonths(1)) monthMap.put(ym, 0);
        for (Object[] r : raw) {
            monthMap.merge(YearMonth.from(((java.sql.Date) r[0]).toLocalDate()),
                    ((Number) r[1]).intValue(), Integer::sum);
        }
        YearMonth now = YearMonth.now();
        int max = monthMap.values().stream().mapToInt(v -> v).max().orElse(1);
        List<SalesAnalysisResDto.SalesBar> bars = new ArrayList<>();
        for (var e : monthMap.entrySet()) {
            YearMonth ym = e.getKey();
            int val = e.getValue();
            String label = ym.getYear() != now.getYear() ? ym.getYear() + "." + ym.getMonthValue() + "월" : ym.getMonthValue() + "월";
            bars.add(SalesAnalysisResDto.SalesBar.builder()
                    .label(label).value(val).height(intRate(val, max)).best(val == max && max > 0).build());
        }
        return bars;
    }

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

    private static int intRate(int value, int total) {
        return total > 0 ? (int) ((long) value * 100 / total) : 0;
    }

    private static double growthRate(int current, int previous) {
        return previous > 0 ? Math.round((double) (current - previous) / previous * 1000.0) / 10.0 : 0;
    }
}