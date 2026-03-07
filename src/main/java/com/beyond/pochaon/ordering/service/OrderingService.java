package com.beyond.pochaon.ordering.service;

import com.beyond.pochaon.common.kafka.KafkaService;
import com.beyond.pochaon.ordering.domain.OrderStatus;
import com.beyond.pochaon.ordering.domain.Ordering;
import com.beyond.pochaon.ordering.dto.MenuDoneDto;
import com.beyond.pochaon.ordering.dto.OrderQueueDto;
import com.beyond.pochaon.ordering.repository.OrderingDetailRepository;
import com.beyond.pochaon.ordering.repository.OrderingRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class OrderingService {

    private final OrderingRepository orderingRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final OrderingDetailRepository orderingDetailRepository;
    private final KafkaService kafkaService;

    @Autowired
    public OrderingService(OrderingRepository orderingRepository, SimpMessagingTemplate simpMessagingTemplate, OrderingDetailRepository orderingDetailRepository, KafkaService kafkaService) {
        this.orderingRepository = orderingRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.orderingDetailRepository = orderingDetailRepository;
        this.kafkaService = kafkaService;
    }

    // 점주 화면 로드시 Stanby 만 조회
    @Transactional(readOnly = true)
    public List<OrderQueueDto> getOrderQueue(Long storeId) {
//       현 매장의 StanBy 주문만 조회
        List<Ordering> standByOrders = orderingRepository.findByStoreIdAndOrderStatus(storeId, OrderStatus.STANDBY);
        if (!standByOrders.isEmpty()) {
            List<Long> orderingIds = standByOrders.stream()
                    .map(Ordering::getId).toList();
            orderingDetailRepository.fetchOptionsForOrderings(orderingIds);
        }
//        엔티티 -> dto 변환
        return standByOrders.stream().map(OrderQueueDto::fromEntity).toList();
    }


    //    점주가 주문 완료 버튼 클릭
    public OrderQueueDto completeOrder(Long orderingId, Long storeId) {
//        주문 조회
        Ordering ordering = orderingRepository.findById(orderingId).orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다. ordering_ser_complete"));

//        해당 매장의 주문인지 권한 검증
        Long orderStoreId = ordering.getCustomerTable().getStore().getId();

        if (!orderStoreId.equals(storeId)) {
            throw new SecurityException("해당 주문에 대한 권한이 없습니다 ordering_ser_complete");
        }
//        standBy 상태만 완료 가능
        if (ordering.getOrderStatus() != OrderStatus.STANDBY) {
            throw new IllegalStateException("standby상태의 주문만 완료할 수 있습니다. ordering_ser_complete, 현재 " + ordering.getOrderStatus());
        }

//        상태 변경 standby -> done
        ordering.updateOrderStatus(OrderStatus.DONE);

//        websocket 알림 -> 프론트에서 해당 orderingId 카드를 큐에서 제거
        simpMessagingTemplate.convertAndSend("/topic/order-queue/" + storeId,
                Map.of("type", "ORDER_DONE", //프론트에서 이 타입으로 분기
                        "orderingId", orderingId)); //어떤 주문이 완료됐는지
        return OrderQueueDto.fromEntity(ordering);
    }

    public int getGroupIdTotal(UUID groupId) {
        return orderingRepository.sumTotalPriceByGroupId(groupId);
    }

//    주방이 메뉴완료버튼 -> 점주에게 kafka
    public void menuDone(MenuDoneDto dto, Long storeId){
        MenuDoneDto payload = MenuDoneDto.builder()
                .menuDoneId(UUID.randomUUID())
                .orderingId(dto.getOrderingId())
                .tableNum(dto.getTableNum())
                .menuName(dto.getMenuName())
                .menuQuantity(dto.getMenuQuantity())
                .menuTotal(dto.getMenuTotal())
                .createAt(LocalDateTime.now())
                .storeId(storeId)
                .menuOptionlist(dto.getMenuOptionlist())
                .build();

        kafkaService.menuDone(payload, storeId);
    }
}
