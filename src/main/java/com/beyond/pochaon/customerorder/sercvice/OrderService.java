package com.beyond.pochaon.customerorder.sercvice;


import com.beyond.pochaon.cart.domain.RedisCartItem;
import com.beyond.pochaon.cart.service.CartService;
import com.beyond.pochaon.common.web.WebPublisher;
import com.beyond.pochaon.customerTable.domain.CustomerTable;
import com.beyond.pochaon.customerTable.repository.CustomerTableRepository;
import com.beyond.pochaon.customerorder.dto.OrderCreateDto;
import com.beyond.pochaon.customerorder.dto.OrderListDto;
import com.beyond.pochaon.menu.domain.Menu;
import com.beyond.pochaon.menu.domain.MenuOption;
import com.beyond.pochaon.menu.domain.MenuOptionDetail;
import com.beyond.pochaon.menu.repository.MenuOptionDetailRepository;
import com.beyond.pochaon.menu.repository.MenuOptionRepository;
import com.beyond.pochaon.menu.repository.MenuRepository;
import com.beyond.pochaon.ordering.domain.*;
import com.beyond.pochaon.ordering.dto.OrderQueueDto;
import com.beyond.pochaon.ordering.repository.OrderingRepository;
import com.beyond.pochaon.present.dto.OwnerEventDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class OrderService {

    private final CartService cartService;
    private final MenuRepository menuRepository;
    private final MenuOptionRepository menuOptionRepository;
    private final OrderingRepository orderingRepository;
    @Qualifier("idempotencyRedisTemplate")
    private final RedisTemplate<String, String> idempotencyRedisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final CustomerTableRepository customerTableRepository;
    @Qualifier("groupRedisTemplate")
    private final RedisTemplate<String, String> groupRedisTemplate;
    private final WebPublisher webPublisher;
    private final MenuOptionDetailRepository menuOptionDetailRepository;


    @Autowired
    public OrderService(CartService cartService, MenuRepository menuRepository, MenuOptionRepository menuOptionRepository, OrderingRepository orderingRepository, @Qualifier("idempotencyRedisTemplate") RedisTemplate<String, String> idempotencyRedisTemplate, SimpMessagingTemplate messagingTemplate, CustomerTableRepository customerTableRepository, @Qualifier("groupRedisTemplate") RedisTemplate<String, String> groupRedisTemplate, WebPublisher webPublisher, MenuOptionDetailRepository menuOptionDetailRepository) {
        this.cartService = cartService;
        this.menuRepository = menuRepository;
        this.menuOptionRepository = menuOptionRepository;
        this.orderingRepository = orderingRepository;
        this.idempotencyRedisTemplate = idempotencyRedisTemplate;
        this.messagingTemplate = messagingTemplate;
        this.customerTableRepository = customerTableRepository;
        this.groupRedisTemplate = groupRedisTemplate;
        this.webPublisher = webPublisher;
        this.menuOptionDetailRepository = menuOptionDetailRepository;

    }

    //  멱등성 로직
    private UUID idempotencyCheck(String redisKey, UUID idempotencyKey) {

//      멱등성생성(redis)
        Boolean locked = idempotencyRedisTemplate.opsForValue()
                .setIfAbsent(redisKey, "Lock", Duration.ofSeconds(3));

//        중복 시
        if (Boolean.FALSE.equals(locked)) {
            Ordering duplicated = orderingRepository.findByIdempotencyKey(idempotencyKey);
            if (duplicated != null) {
                return duplicated.getGroupId();
            }
            throw new IllegalArgumentException("이미 처리 중인 주문입니다");
        }

//      멱등성(db)
        Ordering duplicated = orderingRepository.findByIdempotencyKey(idempotencyKey);
        if (duplicated != null) {
            return duplicated.getGroupId();
        }

        return null;
    }


    // 주문공통로직
    private UUID createOrderInternal(OrderCreateDto createDto, UUID groupId, Long tableId, int tableNum, Long storeId) {

//        redis cart조회
        List<RedisCartItem> cartItemList = cartService.cartItems(tableId);
        if (cartItemList.isEmpty()) {
            throw new IllegalArgumentException("장바구니가 비었습니다");
        }

//      테이블 조회
        CustomerTable customerTable = customerTableRepository.findById(tableId).orElseThrow(() -> new IllegalArgumentException("없는 테이블입니다"));

        if (!(customerTable.getTableNum() == (tableNum))) {
            throw new IllegalStateException("테이블 정보가 일치하지 않습니다");
        }

        if (!customerTable.getStore().getId().equals(storeId)) {
            throw new IllegalStateException("매장 정보가 일치하지 않습니다");
        }


//       주문생성
        Ordering ordering = Ordering.builder()
                .customerTable(customerTable)
                .groupId(groupId)
                .idempotencyKey(createDto.getIdempotencyKey())
                .orderStatus(OrderStatus.STANDBY)
                .build();

        int totalPrice = 0;

//        주문상세 // cart에서 값 받아오기
        for (RedisCartItem cartItem : cartItemList) {
            Menu menu = menuRepository.findById(cartItem.getMenuId()).orElseThrow(() -> new IllegalArgumentException("없는 메뉴"));
            int quantity = cartItem.getQuantity();

            OrderingDetail detail = OrderingDetail.builder()
                    .orderingDetailQuantity(quantity)
                    .menu(menu)
                    .ordering(ordering)
                    .menuPrice(menu.getPrice())
                    .build();

//          옵션, 옵션 디테일 조립
            String optionKey = cartItem.getOptionKey(); // ("\\|,2)[1]한 값
            if (optionKey != null && !optionKey.isBlank()) {
                String[] optionArr = optionKey.split("\\|"); // option끼리 구별
                for (String opt : optionArr) {
                    String[] optionSplit = opt.split(":");
                    Long optionId = Long.parseLong(optionSplit[0].trim());
                    MenuOption option = menuOptionRepository.findById(optionId)
                            .orElseThrow(() -> new IllegalArgumentException("없는 옵션입니다"));
//                    optionDto 조립
                    OrderingDetailOption optionSnap = OrderingDetailOption.builder()
                            .orderingOptionName(option.getOptionName())
                            .orderingDetail(detail)
                            .build();

                    detail.getOrderingDetailOptions().add(optionSnap);


                    //옵션 디테일 조립
                    if (optionSplit.length > 1 && !optionSplit[1].isBlank()) {

                        String[] detailArr = optionSplit[1].split(",");

                        for (String d : detailArr) {

                            Long detailId = Long.parseLong(d.trim());

                            MenuOptionDetail optionDetail = menuOptionDetailRepository.findById(detailId).orElseThrow(() -> new IllegalArgumentException("없는 옵션 디테일"));

                            OrderingDetailOptionDetail detailSnap =
                                    OrderingDetailOptionDetail.builder()
                                            .optionDetailName(optionDetail.getOptionDetailName())
                                            .optionDetailPrice(optionDetail.getOptionDetailPrice())
                                            .detailOption(optionSnap)
                                            .build();

                            optionSnap.getOrderingDetailOptionDetails().add(detailSnap);
                        }
                    }
                }
            }
            ordering.getOrderDetail().add(detail);


//            totalPrice금액누적
            int unitPrice = cartItem.getUnitPrice();
            totalPrice += unitPrice * quantity;
        }
//        주문 총액 스냅샷
        ordering.setTotalPrice(totalPrice);

//        db저장
        orderingRepository.save(ordering);

//        publisher호출
        OrderCreateDto orderCreateDto = buildWebDto(ordering, customerTable);

        OwnerEventDto eventDto = OwnerEventDto.builder()
                .eventType("ORDER")
                .storeId(customerTable.getStore().getId())
                .payload(orderCreateDto)
                .build();
        webPublisher.publish(eventDto);


//          실시간 주문 알림(테이블 단위)
//            // ========== 점주 화면에 실시간 알림 ==========
        messagingTemplate.convertAndSend(
                "/topic/order-queue/" + storeId,
                Map.of(
                        "type", "NEW_ORDER",
                        "order", OrderQueueDto.fromEntity(ordering)
                )
        );


//        카트 비우기
        cartService.CartClear(tableId);

        return groupId;
    }


    //  1.주문생성
    public UUID create(OrderCreateDto createDto, int tableNum, Long tableId, Long storeId) {

        String redisKey = "idempotency:order:create:" + createDto.getIdempotencyKey();

//      멱등성(redis) -동시성 제어
        UUID duplicatedGroupId = idempotencyCheck(redisKey, createDto.getIdempotencyKey());

        if (duplicatedGroupId != null) {
            return duplicatedGroupId;
        }

        try {
//          redis. group uuid
            String groupKey = String.valueOf(tableNum);
            String groupValue = groupRedisTemplate.opsForValue().get(groupKey);

//      주문묶음
            UUID groupId;
            if (groupValue != null) {
                groupId = UUID.fromString(groupValue);
            } else {
                groupId = UUID.randomUUID();
            }

            UUID result = createOrderInternal(createDto, groupId, tableId, tableNum, storeId);

//            groupId redis저장
            groupRedisTemplate.opsForValue().set(groupKey, groupId.toString(), Duration.ofHours(4));

//       db저장 성공 시 상태 확인용- 재전송방지
            idempotencyRedisTemplate.opsForValue().set(redisKey, "SUCCESS", Duration.ofMinutes(1));
            return result;

        } catch (Exception e) {
            idempotencyRedisTemplate.delete(redisKey);
            throw e;
        }
    }


    //    2. 추가주문
    public UUID add(OrderCreateDto createDto, UUID groupId, int tableNum, Long tableId, Long storeId) {

        //      기존 주문 유무 조회
        if (groupId == null) {
            throw new IllegalArgumentException("groupId가 필요합니다.");
        }

        List<Ordering> exist = orderingRepository.findByGroupId(groupId);

        if (exist == null || exist.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 GroupId입니다");
        }
        Ordering first = exist.get(0);


        if (!(first.getCustomerTable().getTableNum() == (tableNum))) {
            throw new IllegalStateException("다른 테이블의 주문 그룹입니다.");
        }

        if (first.getOrderStatus() == OrderStatus.DONE || first.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("이미 종료된 주문 그룹입니다.");
        }


        String redisKey = "idempotency:order:add:" + createDto.getIdempotencyKey();
//      멱등성 처리
        UUID duplicateGroupId = idempotencyCheck(redisKey, createDto.getIdempotencyKey());
        if (duplicateGroupId != null) {
            return duplicateGroupId;
        }
        try {
//            주문생성로직 호출
            UUID result = createOrderInternal(createDto, groupId,  tableId, tableNum, storeId);

//          db저장 성공 시 상태 확인용 (재시도 방지)
            idempotencyRedisTemplate.opsForValue().set(redisKey, "SUCCESS", Duration.ofMinutes(1));

            return result;

        } catch (Exception e) {

//       주문처리중 예외 발생시 lock해제 (재시도)
            idempotencyRedisTemplate.delete(redisKey);

            throw e;
        }
    }

    //    3. 주문내역 조회
    public List<OrderListDto> list(UUID groupId, int tableNum) {
//
        if (groupId == null) {
            throw new IllegalArgumentException("groupId가 필요합니다.");
        }
        //        groupId조회
        List<Ordering> orderings = orderingRepository.findByGroupId(groupId);

        if (orderings == null || orderings.isEmpty()) {
            throw new IllegalArgumentException("주문내역이 없습니다");
        }

        Ordering first = orderings.get(0);

        if (!(first.getCustomerTable().getTableNum() ==(tableNum))) {
            throw new IllegalStateException("다른 테이블의 주문 그룹입니다.");
        }

//      주문 조립
        List<OrderListDto> result = new ArrayList<>();

        for (Ordering ordering : orderings) {

            List<OrderListDto.OrderListDetailDto> detailDtoList = new ArrayList<>();

            for (OrderingDetail detail : ordering.getOrderDetail()) {
                int quantity = detail.getOrderingDetailQuantity();
                int menuPrice = detail.getMenuPrice();
                int optionSum = 0;

                List<OrderListDto.OrderListDetailDto.OrderListDetailOptionDto> optionDtoList = new ArrayList<>();

                for (OrderingDetailOption option : detail.getOrderingDetailOptions()) {

                    List<OrderListDto.OrderListDetailDto.OrderListDetailOptionDto.OrderDetailOptionDetailDto> detailOptionDtoList = new ArrayList<>();

                    for (OrderingDetailOptionDetail optionDetail : option.getOrderingDetailOptionDetails()) {
                        optionSum += optionDetail.getOptionDetailPrice();

//                        옵션 디테일 조립
                        detailOptionDtoList.add(OrderListDto.OrderListDetailDto.OrderListDetailOptionDto.OrderDetailOptionDetailDto.builder()
                                .optionDetailId(optionDetail.getId())
                                .optionDetailName(optionDetail.getOptionDetailName())
                                .build()
                        );
                    }
//                        옵션 조립
                    optionDtoList.add(OrderListDto.OrderListDetailDto.OrderListDetailOptionDto.builder()
                            .optionId(option.getId())
                            .optionName(option.getOrderingOptionName())
                            .orderDetailOptionDetailDto(detailOptionDtoList)
                            .build()

                    );
                }
                int unitPrice = menuPrice + optionSum;
                int linePrice = unitPrice * quantity;
//                  메뉴 조립
                detailDtoList.add(OrderListDto.OrderListDetailDto.builder()
                        .menuId(detail.getMenu().getId())
                        .menuName(detail.getMenu().getMenuName())
                        .menuQuantity(quantity)
                        .linePrice(linePrice)
                        .orderDetailOpDto(optionDtoList)
                        .build()
                );
            }
//                주문정보 조립
            result.add(OrderListDto.builder()
                    .tableId((long) ordering.getCustomerTable().getTableNum())
                    .groupId(ordering.getGroupId())
                    .totalPrice(ordering.getTotalPrice())
                    .listDetailDto(detailDtoList)
                    .build()
            );
        }

        return result;
    }


    //create pubsub 조립
    private OrderCreateDto buildWebDto(Ordering ordering, CustomerTable customerTable) {

        List<OrderCreateDto.WebMenu> webMenuList = new ArrayList<>();

        // 메뉴단위 주문 조립 // DB entity->web dto
        for (OrderingDetail detail : ordering.getOrderDetail()) {

            List<OrderCreateDto.WebMenu.Option> optionList = new ArrayList<>();

            // 옵션단위 주문조립
            for (OrderingDetailOption option : detail.getOrderingDetailOptions()) {

                List<OrderCreateDto.WebMenu.Option.OptionDetail> detailList = new ArrayList<>();

                // 옵션 디테일 단위 주문 조립
                for (OrderingDetailOptionDetail optionDetail : option.getOrderingDetailOptionDetails()) {

                    detailList.add(
                            OrderCreateDto.WebMenu.Option.OptionDetail.builder()
                                    .optionDetailName(optionDetail.getOptionDetailName())
                                    .optionDetailPrice(optionDetail.getOptionDetailPrice())
                                    .build()
                    );
                }

                OrderCreateDto.WebMenu.Option webOption = OrderCreateDto.WebMenu.Option.builder()
                        .optionGroupName(option.getOrderingOptionName())
                        .optionDetailList(detailList)
                        .build();

                optionList.add(webOption);
            }


            OrderCreateDto.WebMenu webMenu = OrderCreateDto.WebMenu.builder()
                    .menuName(detail.getMenu().getMenuName())
                    .quantity(detail.getOrderingDetailQuantity())
                    .optionList(optionList)
                    .menuPrice(detail.getMenuPrice())
                    .build();

            webMenuList.add(webMenu);
        }

        return OrderCreateDto.builder()
                .orderingId(ordering.getId())
                .tableNumber(customerTable.getTableNum())
                .groupId(ordering.getGroupId())
                .idempotencyKey(ordering.getIdempotencyKey())
                .webMenuList(webMenuList)
                .build();
    }
}
