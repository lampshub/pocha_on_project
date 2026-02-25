package com.beyond.pochaon.ordering.service;

import com.beyond.pochaon.ordering.domain.OrderStatus;
import com.beyond.pochaon.ordering.domain.Ordering;
import com.beyond.pochaon.ordering.dto.OrderQueueDto;
import com.beyond.pochaon.ordering.repository.OrderingRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class OrderingService {

    private final OrderingRepository orderingRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    public OrderingService(OrderingRepository orderingRepository, SimpMessagingTemplate simpMessagingTemplate) {
        this.orderingRepository = orderingRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    /*
    // ========== 점주 화면에 실시간 알림 ==========
        Long storeId = table.getStore().getId();
        messagingTemplate.convertAndSend(
                "/topic/order-queue/" + storeId,
                Map.of(
                        "type", "NEW_ORDER",
                        "order", OrderQueueDto.fromEntity(ordering)
        )
     );
     */
    //    주문취소리스트조회
    public List<Ordering> cancelledList() {
        return orderingRepository.findByOrderStatusOrderByIdDesc(OrderStatus.CANCELLED);
    }

    // 점주 화면 로드시 Stanby 만 조회
    @Transactional(readOnly = true)
    public List<OrderQueueDto> getOrderQueue(Long storeId) {
//       현 매장의 StanBy 주문만 조회
        List<Ordering> standByOrders = orderingRepository.findByStoreIdAndOrderStatus(storeId, OrderStatus.STANDBY);
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

//    //        websocket 선물알림 -> 프론트에서 해당 orderingId 카드를 큐에서 제거
//        simpMessagingTemplate.convertAndSend("/topic/order-queue/" + storeId,
//            Map.of("type", "ORDER_DONE", //프론트에서 이 타입으로 분기
//            "orderingId", orderingId)); //어떤 주문이 완료됐는지
//        return OrderQueueDto.fromEntity(ordering);
//}

    public int getGroupIdTotal(UUID groupId) {
        return orderingRepository.sumTotalPriceByGroupId(groupId);
    }
}
