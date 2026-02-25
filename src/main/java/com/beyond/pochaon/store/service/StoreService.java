package com.beyond.pochaon.store.service;


import com.beyond.pochaon.common.auth.JwtTokenProvider;
import com.beyond.pochaon.ordering.domain.Ordering;
import com.beyond.pochaon.ordering.domain.OrderingDetail;
import com.beyond.pochaon.ordering.repository.OrderingRepository;
import com.beyond.pochaon.owner.domain.Owner;
import com.beyond.pochaon.owner.domain.Role;
import com.beyond.pochaon.owner.repository.OwnerRepository;
import com.beyond.pochaon.payment.entity.Payment;
import com.beyond.pochaon.payment.repository.PaymentRepository;
import com.beyond.pochaon.store.domain.Store;
import com.beyond.pochaon.store.domain.StoreSettlement;
import com.beyond.pochaon.store.dtos.*;
import com.beyond.pochaon.store.repository.StoreRepository;
import com.beyond.pochaon.store.repository.StoreSettlementRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class StoreService {

    private final StoreRepository storeRepository;
    private final OwnerRepository ownerRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final StoreSettlementRepository storeSettlementRepository;
    private final OrderingRepository orderingRepository;
    private final PaymentRepository paymentRepository;

    @Autowired
    public StoreService(StoreRepository storeRepository, OwnerRepository ownerRepository, JwtTokenProvider jwtTokenProvider, StoreSettlementRepository storeSettlementRepository, OrderingRepository orderingRepository, PaymentRepository paymentRepository) {
        this.storeRepository = storeRepository;
        this.ownerRepository = ownerRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.storeSettlementRepository = storeSettlementRepository;
        this.orderingRepository = orderingRepository;
        this.paymentRepository = paymentRepository;
    }


    public void createStore(StoreCreateDto dto, String email) {
        Owner owner = ownerRepository.findByOwnerEmail(email).orElseThrow(() -> new EntityNotFoundException("없는 이메일입니다. store_ser_create부분"));
        Store store = Store.builder()
                .storeName(dto.getName())
                .address(dto.getAddress())
                .storeOpenAt(dto.getOpenAt())
                .storeCloseAt(dto.getCloseAt())
                .owner(owner)
                .build();
        storeRepository.save(store);
    }

    @Transactional(readOnly = true)
    public List<StoreListDto> findAll(String email) {
        Owner owner = ownerRepository.findByOwnerEmail(email).orElseThrow(() -> new EntityNotFoundException("없는 entity. store_ser_findAll"));
        List<Store> storeList;
        if (owner.getRole() == Role.ADMIN) {
            storeList = storeRepository.findAll();
        } else {
            storeList = storeRepository.findByOwner(owner);
        }
        return storeList.stream().map(StoreListDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StoreTokenDto selectStore(String email, Long storeId) {

        Owner owner = ownerRepository.findByOwnerEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Owner not found"));

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("Store not found"));

        //  소유자 검증
        if (!store.getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("해당 매장에 대한 권한이 없습니다.");
        }

        String storeAccessToken =
                jwtTokenProvider.createStoreAccessToken(owner, storeId);

        return new StoreTokenDto(storeAccessToken);
    }

    @Transactional(readOnly = true)
    public StoreTimeResDto getStoreHours(Long storeId) {
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new IllegalStateException("Store not found"));
        return new StoreTimeResDto(store.getStoreOpenAt(), store.getStoreCloseAt());
    }

    @Transactional
    public void updateTime(Long storeId, StoreUpdateTimeDto dto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Owner owner = ownerRepository.findByOwnerEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Owner not found"));
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("Store not found"));
        if (!store.getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("해당 매장에 대한 권한이 없습니다");
        }
        if (dto.getOpenAt().equals(dto.getCloseAt())) {
            throw new IllegalStateException("오픈시간과 마감시간은 같을 수 없습니다");
        }

        store.updateTime(dto.getOpenAt(), dto.getCloseAt());
    }

    //    달력 탭
    @Transactional(readOnly = true)
    public MonthlyCalenderResDto getMonthlyCalender(Long storeId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.plusMonths(1).atDay(1);

        List<StoreSettlement> settlements = storeSettlementRepository
                .findByStoreIdAndSettlementDateRange(storeId, startDate, endDate);

        Map<Integer, Integer> dailySales = new HashMap<>();
        int totalOrderCount = 0;
        int totalCancelCount = 0;

        for (StoreSettlement ss : settlements) {
            dailySales.put(ss.getSettlementDate().getDayOfMonth(), ss.getDayTotalAmount());
            totalOrderCount += ss.getOrderCount();
            totalCancelCount += ss.getCancelCount();
        }

        return MonthlyCalenderResDto.builder()
                .year(year)
                .month(month)
                .dailySales(dailySales)
                .orderCount(totalOrderCount)
                .cancelCount(totalCancelCount)
                .build();
    }

    //    달력 일별 상세
    @Transactional(readOnly = true)
    public SimpleSettlementDto getDailySettlement(Long storeId, int year, int month, int day) {
        LocalDate date = LocalDate.of(year, month, day);

        return storeSettlementRepository.findByStoreIdAndSettlementDate(storeId, date)
                .map(ss -> SimpleSettlementDto.builder()
                        .dayTotal(ss.getDayTotalAmount())
                        .orderCount(ss.getOrderCount())
                        .averageOrderAmount(ss.getAverageOrderAmount())
                        .cancelCount(ss.getCancelCount())
                        .refundAmount(ss.getRefundAmount())
                        .netSales(ss.getNetSales())
                        .cardSales(ss.getCardSales())
                        .cashSales(ss.getCashSales())
                        .transferSales(ss.getTransferSales())
                        .tableUseCount(ss.getTableUseCount())
                        .build())
                .orElse(SimpleSettlementDto.builder()
                        .dayTotal(0).orderCount(0).averageOrderAmount(0)
                        .cancelCount(0).refundAmount(0).netSales(0)
                        .cardSales(0).cashSales(0).transferSales(0).tableUseCount(0)
                        .build());
    }

    //    메뉴 분석
    @Transactional(readOnly = true)
    public MenuAnalysisResDto getMenuAnalysis(Long storeId, int year, int month, int day) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime startAt = day > 0
                ? ym.atDay(day).atStartOfDay()
                : ym.atDay(1).atStartOfDay();
        LocalDateTime endAt = day > 0
                ? ym.atDay(day).atStartOfDay().plusDays(1)
                : ym.plusMonths(1).atDay(1).atStartOfDay();

        List<Ordering> orders = orderingRepository
                .findCompletedOrdersWithDetails(storeId, startAt, endAt);

        // 카테고리별 매출 집계
        Map<String, Integer> categoryMap = new LinkedHashMap<>();
        // 메뉴별 판매량/매출 집계
        Map<String, int[]> menuMap = new LinkedHashMap<>(); // [qty, amount]
        // 조합 분석용: 주문(groupId)별 메뉴 이름 수집
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

        // 카테고리별 매출 → DTO (최대값 대비 비율)
        int maxCatAmount = categoryMap.values().stream().mapToInt(v -> v).max().orElse(1);
        List<MenuAnalysisResDto.CategorySales> categorySales = categoryMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> MenuAnalysisResDto.CategorySales.builder()
                        .name(e.getKey())
                        .amount(e.getValue())
                        .rate(maxCatAmount > 0 ? (int) ((long) e.getValue() * 100 / maxCatAmount) : 0)
                        .build())
                .collect(Collectors.toList());

        // 메뉴별 순위 → DTO (전체 매출 대비 비율)
        int totalMenuAmount = menuMap.values().stream().mapToInt(v -> v[1]).sum();
        List<MenuAnalysisResDto.MenuRank> menuRanking = menuMap.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue()[1], a.getValue()[1]))
                .map(e -> MenuAnalysisResDto.MenuRank.builder()
                        .name(e.getKey())
                        .qty(e.getValue()[0])
                        .amount(e.getValue()[1])
                        .rate(totalMenuAmount > 0 ? (int) ((long) e.getValue()[1] * 100 / totalMenuAmount) : 0)
                        .build())
                .collect(Collectors.toList());

        // 조합 분석: 2개 메뉴 조합 빈도
        Map<String, Integer> comboMap = new HashMap<>();
        for (Set<String> menus : groupMenus.values()) {
            List<String> menuList = new ArrayList<>(menus);
            Collections.sort(menuList);
            for (int i = 0; i < menuList.size(); i++) {
                for (int j = i + 1; j < menuList.size(); j++) {
                    String pair = menuList.get(i) + " + " + menuList.get(j);
                    comboMap.merge(pair, 1, Integer::sum);
                }
            }
        }
        List<MenuAnalysisResDto.MenuCombo> combos = comboMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(e -> MenuAnalysisResDto.MenuCombo.builder()
                        .pair(e.getKey())
                        .count(e.getValue())
                        .build())
                .collect(Collectors.toList());

        return MenuAnalysisResDto.builder()
                .categorySales(categorySales)
                .menuRanking(menuRanking)
                .combos(combos)
                .build();
    }


    //    매출 분석 탭
    @Transactional(readOnly = true)
    public SalesAnalysisResDto getSalesAnalysis(Long storeId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.plusMonths(1).atDay(1);

        List<StoreSettlement> settlements = storeSettlementRepository
                .findByStoreIdAndSettlementDateRange(storeId, startDate, endDate);

        List<SalesAnalysisResDto.SalesBar> dailyBars = buildDailyBars(settlements);
        List<SalesAnalysisResDto.SalesBar> weeklyBars = buildWeeklyBars(settlements, ym);
        List<SalesAnalysisResDto.SalesBar> monthlyBars = buildMonthlyBars(storeId, year);

        // 시간대별/요일별은 ordering 직접 조회라 LocalDateTime 유지
        LocalDateTime startAt = ym.atDay(1).atStartOfDay();
        LocalDateTime endAt = ym.plusMonths(1).atDay(1).atStartOfDay();
        List<Object[]> hourlyRaw = orderingRepository.sumSalesByHour(storeId, startAt, endAt);
        List<SalesAnalysisResDto.HourlySales> hourlySales = buildHourlySales(hourlyRaw);

        List<Object[]> dowRaw = orderingRepository.sumSalesByDayOfWeek(storeId, startAt, endAt);
        List<SalesAnalysisResDto.SalesBar> dayOfWeekSales = buildDayOfWeekSales(dowRaw);

        SalesAnalysisResDto.SalesCompare compare = buildSalesCompare(storeId, year, month);

        return SalesAnalysisResDto.builder()
                .dailyBars(dailyBars)
                .weeklyBars(weeklyBars)
                .monthlyBars(monthlyBars)
                .hourlySales(hourlySales)
                .dayOfWeekSales(dayOfWeekSales)
                .compare(compare)
                .build();
    }

    private List<SalesAnalysisResDto.SalesBar> buildDailyBars(List<StoreSettlement> settlements) {
        if (settlements.isEmpty()) return Collections.emptyList();
        int max = settlements.stream().mapToInt(StoreSettlement::getDayTotalAmount).max().orElse(1);
        return settlements.stream()
                .map(ss -> SalesAnalysisResDto.SalesBar.builder()
                        .label(ss.getSettlementDate().getDayOfMonth() + "일")  // ✅
                        .value(ss.getDayTotalAmount())
                        .height(max > 0 ? (int) ((long) ss.getDayTotalAmount() * 100 / max) : 0)
                        .best(ss.getDayTotalAmount() == max)
                        .build())
                .collect(Collectors.toList());
    }

    private List<SalesAnalysisResDto.SalesBar> buildWeeklyBars(List<StoreSettlement> settlements, YearMonth ym) {
        Map<Integer, Integer> weekMap = new LinkedHashMap<>();
        for (StoreSettlement ss : settlements) {
            int day = ss.getSettlementDate().getDayOfMonth();  // ✅
            int week = (day - 1) / 7 + 1;
            weekMap.merge(week, ss.getDayTotalAmount(), Integer::sum);
        }
        int max = weekMap.values().stream().mapToInt(v -> v).max().orElse(1);
        return weekMap.entrySet().stream()
                .map(e -> SalesAnalysisResDto.SalesBar.builder()
                        .label(e.getKey() + "주")
                        .value(e.getValue())
                        .height(max > 0 ? (int) ((long) e.getValue() * 100 / max) : 0)
                        .best(e.getValue() == max)
                        .build())
                .collect(Collectors.toList());
    }

    private List<SalesAnalysisResDto.SalesBar> buildMonthlyBars(Long storeId, int year) {
        List<SalesAnalysisResDto.SalesBar> bars = new ArrayList<>();
        int max = 0;
        int[] monthlyValues = new int[12];

        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(year, m);
            LocalDate s = ym.atDay(1);
            LocalDate e = ym.plusMonths(1).atDay(1);
            List<StoreSettlement> list = storeSettlementRepository
                    .findByStoreIdAndSettlementDateRange(storeId, s, e);
            int total = list.stream().mapToInt(StoreSettlement::getDayTotalAmount).sum();
            monthlyValues[m - 1] = total;
            if (total > max) max = total;
        }

        for (int m = 0; m < 12; m++) {
            int finalMax = max;
            bars.add(SalesAnalysisResDto.SalesBar.builder()
                    .label((m + 1) + "월")
                    .value(monthlyValues[m])
                    .height(finalMax > 0 ? (int) ((long) monthlyValues[m] * 100 / finalMax) : 0)
                    .best(monthlyValues[m] == max && max > 0)
                    .build());
        }
        return bars;
    }

    private int getMonthTotal(Long storeId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate s = ym.atDay(1);
        LocalDate e = ym.plusMonths(1).atDay(1);
        return storeSettlementRepository.findByStoreIdAndSettlementDateRange(storeId, s, e)  // ✅
                .stream().mapToInt(StoreSettlement::getDayTotalAmount).sum();
    }

    private List<SalesAnalysisResDto.HourlySales> buildHourlySales(List<Object[]> raw) {
        Map<Integer, Integer> hourMap = new LinkedHashMap<>();
        for (Object[] row : raw) {
            int hour = ((Number) row[0]).intValue();
            int amount = ((Number) row[1]).intValue();
            hourMap.put(hour, amount);
        }
        int max = hourMap.values().stream().mapToInt(v -> v).max().orElse(1);
        List<SalesAnalysisResDto.HourlySales> result = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            int val = hourMap.getOrDefault(h, 0);
            result.add(SalesAnalysisResDto.HourlySales.builder()
                    .hour(h)
                    .amount(val)
                    .height(max > 0 ? (int) ((long) val * 100 / max) : 0)
                    .build());
        }
        return result;
    }

    private List<SalesAnalysisResDto.SalesBar> buildDayOfWeekSales(List<Object[]> raw) {
        // MySQL DAYOFWEEK: 1=일, 2=월, ..., 7=토
        String[] labels = {"일", "월", "화", "수", "목", "금", "토"};
        int[] values = new int[7];
        for (Object[] row : raw) {
            int dow = ((Number) row[0]).intValue(); // 1~7
            int amount = ((Number) row[1]).intValue();
            values[dow - 1] = amount;
        }
        int max = Arrays.stream(values).max().orElse(1);
        List<SalesAnalysisResDto.SalesBar> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            result.add(SalesAnalysisResDto.SalesBar.builder()
                    .label(labels[i])
                    .value(values[i])
                    .height(max > 0 ? (int) ((long) values[i] * 100 / max) : 0)
                    .best(values[i] == max && max > 0)
                    .build());
        }
        return result;
    }

    private SalesAnalysisResDto.SalesCompare buildSalesCompare(Long storeId, int year, int month) {
        // 이번 달
        int thisMonth = getMonthTotal(storeId, year, month);

        // 전월
        YearMonth prev = YearMonth.of(year, month).minusMonths(1);
        int lastMonth = getMonthTotal(storeId, prev.getYear(), prev.getMonthValue());

        // 전년 동월
        int lastYear = getMonthTotal(storeId, year - 1, month);

        double momRate = lastMonth > 0 ? Math.round((double) (thisMonth - lastMonth) / lastMonth * 1000.0) / 10.0 : 0;
        double yoyRate = lastYear > 0 ? Math.round((double) (thisMonth - lastYear) / lastYear * 1000.0) / 10.0 : 0;

        return SalesAnalysisResDto.SalesCompare.builder()
                .thisMonth(thisMonth)
                .lastMonth(lastMonth)
                .momRate(momRate)
                .lastYear(lastYear)
                .yoyRate(yoyRate)
                .build();
    }


    //    결제 분석 탭
    @Transactional(readOnly = true)
    public PaymentAnalysisResDto getPaymentAnalysis(Long storeId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime startAt = ym.atDay(1).atStartOfDay();
        LocalDateTime endAt = ym.plusMonths(1).atDay(1).atStartOfDay();

        // 결제 수단별 집계
        List<Object[]> methodRaw = paymentRepository.sumByPaymentMethod(storeId, startAt, endAt);
        int totalAmount = 0;
        int totalCount = 0;
        int cardAmount = 0, easyPayAmount = 0, transferAmount = 0, phoneAmount = 0;
        int cardCount = 0, easyPayCount = 0, transferCount = 0, phoneCount = 0; // ✅ 추가

        for (Object[] row : methodRaw) {
            String method = (String) row[0];
            int amount = ((Number) row[1]).intValue();
            int count = ((Number) row[2]).intValue();
            totalAmount += amount;
            totalCount += count;

            if ("카드".equals(method)) {
                cardAmount = amount;
                cardCount = count;
            } else if ("간편결제".equals(method)) {
                easyPayAmount = amount;
                easyPayCount = count;
            } else if ("계좌이체".equals(method)) {
                transferAmount = amount;
                transferCount = count;
            } else if ("휴대폰".equals(method)) {
                phoneAmount = amount;
                phoneCount = count;
            }
        }
        int cardRate = totalCount > 0 ? (int) Math.round((double) cardCount / totalCount * 100) : 0;
        int easyPayRate = totalCount > 0 ? (int) Math.round((double) easyPayCount / totalCount * 100) : 0;
        int transferRate = totalCount > 0 ? (int) Math.round((double) transferCount / totalCount * 100) : 0;
        int phoneRate = totalCount > 0 ? (int) Math.round((double) phoneCount / totalCount * 100) : 0;

        PaymentAnalysisResDto.PaymentMethodBreakdown breakdown = PaymentAnalysisResDto.PaymentMethodBreakdown.builder()
                .cardRate(cardRate)
                .easyPayRate(easyPayRate)
                .transferRate(transferRate)
                .phoneRate(phoneRate)
                .build();

        int avgAmount = totalCount > 0 ? totalAmount / totalCount : 0;
        PaymentAnalysisResDto.PaymentSummary summary = PaymentAnalysisResDto.PaymentSummary.builder()
                .avgAmount(avgAmount).totalCount(totalCount).totalAmount(totalAmount).build();

        // 최근 결제 내역 (메뉴 상세 포함)
        List<Payment> recentPayments = paymentRepository.findRecentTransactions(storeId, startAt, endAt);

        List<PaymentAnalysisResDto.TransactionDetail> transactions = new ArrayList<>();
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        for (Payment payment : recentPayments) {
            // groupId로 주문 메뉴 조회
            List<PaymentAnalysisResDto.TransactionMenu> menus = new ArrayList<>();
            if (payment.getGroupId() != null) {
                try {
                    UUID groupId = UUID.fromString(payment.getGroupId());
                    List<Ordering> groupOrders = orderingRepository.findByGroupId(groupId);
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

            String methodLabel = payment.getMethod() != null ? payment.getMethod() : "기타";

            transactions.add(PaymentAnalysisResDto.TransactionDetail.builder()
                    .id(payment.getId())
                    .method(methodLabel)
                    .time(payment.getApproveAt() != null ? payment.getApproveAt().format(timeFmt) : "")
                    .amount(payment.getAmount())
                    .tableNum(payment.getTableNum() != null ? payment.getTableNum() : 0)
                    .menus(menus)
                    .build());
        }

        return PaymentAnalysisResDto.builder()
                .methodBreakdown(breakdown)
                .summary(summary)
                .recentTransactions(transactions)
                .build();
    }

    //    테이블 분석 탭
    @Transactional(readOnly = true)
    public TableAnalysisResDto getTableAnalysis(Long storeId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime startAt = ym.atDay(1).atStartOfDay();
        LocalDateTime endAt = ym.plusMonths(1).atDay(1).atStartOfDay();

        // 오늘 범위
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();

        // 테이블별 매출/건수
        List<Object[]> tableSalesRaw = orderingRepository.sumSalesByTable(storeId, startAt, endAt);
        int maxTableAmount = tableSalesRaw.stream()
                .mapToInt(r -> ((Number) r[1]).intValue()).max().orElse(1);

        List<TableAnalysisResDto.TableSales> tableSales = tableSalesRaw.stream()
                .map(row -> TableAnalysisResDto.TableSales.builder()
                        .tableNum(((Number) row[0]).intValue())
                        .amount(((Number) row[1]).intValue())
                        .count(((Number) row[2]).intValue())
                        .rate(maxTableAmount > 0 ? (int) ((long) ((Number) row[1]).intValue() * 100 / maxTableAmount) : 0)
                        .build())
                .collect(Collectors.toList());

        // 테이블별 회전율 (해당 월)
        List<Object[]> turnoverRaw = orderingRepository.countGroupsByTable(storeId, startAt, endAt);
        int daysInMonth = ym.lengthOfMonth();
        int daysElapsed = today.getMonthValue() == month && today.getYear() == year
                ? today.getDayOfMonth() : daysInMonth;

        List<TableAnalysisResDto.TableTurnover> tableTurnover = turnoverRaw.stream()
                .map(row -> {
                    int groups = ((Number) row[1]).intValue();
                    double turnover = daysElapsed > 0
                            ? Math.round((double) groups / daysElapsed * 10.0) / 10.0 : 0;
                    return TableAnalysisResDto.TableTurnover.builder()
                            .tableNum(((Number) row[0]).intValue())
                            .turnover(turnover)
                            .build();
                })
                .collect(Collectors.toList());

        // 오늘 총 이용 횟수
        int todayUseCount = orderingRepository.countDistinctGroupIds(storeId, todayStart, todayEnd);

        // 평균 회전율
        double avgTurnover = tableTurnover.stream()
                .mapToDouble(TableAnalysisResDto.TableTurnover::getTurnover)
                .average().orElse(0);
        avgTurnover = Math.round(avgTurnover * 10.0) / 10.0;

        // 평균 이용 시간 (추정: 총매출 / 건수 기반, 정확한 계산은 세션 추적 필요)
        int totalOrders = tableSales.stream().mapToInt(TableAnalysisResDto.TableSales::getCount).sum();
        int avgDuration = totalOrders > 0 ? 75 : 0; // 기본 추정값 (정확한 값은 테이블 세션 추적 시 개선)

        TableAnalysisResDto.TableSummary summary = TableAnalysisResDto.TableSummary.builder()
                .avgTurnover(avgTurnover)
                .avgDuration(avgDuration)
                .todayUseCount(todayUseCount)
                .build();

        return TableAnalysisResDto.builder()
                .summary(summary)
                .tableSales(tableSales)
                .tableTurnover(tableTurnover)
                .build();
    }
}
