package com.beyond.pochaon.ordering.dto;

//점주 화면 하단에 보여줄 주문 리스트

import com.beyond.pochaon.ordering.domain.OrderStatus;
import com.beyond.pochaon.ordering.domain.Ordering;
import com.beyond.pochaon.ordering.domain.OrderingDetail;
import com.beyond.pochaon.ordering.domain.OrderingDetailOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderQueueDto {
    private Long orderingId; // 주문 ID
    private Long tableId; // 테이블 번호
    private int totalPrice; // 주문 총액
    private OrderStatus orderStatus; //주문 상태
    private LocalDateTime createAt; // 주문 시간 (basetime으로 생성된 시간 )
    private List<OrderingDetailInfo> orderingDetailInfos; //주문 상품 리스트

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderingDetailInfo {
        private String menuName; //메뉴명
        private Long quantity; //수량
        private List<String> option; //옵션명 리스트
    }

    //    ordering - dto로
    public static OrderQueueDto fromEntity(Ordering ordering) {
//        각 주문 상품(orderingDetail)을 dto로 변환
        List<OrderingDetailInfo> details = ordering.getOrderDetail().stream().map(OrderQueueDto::toDetailInfo).toList();
        return OrderQueueDto.builder()
                .orderingId(ordering.getId())
                .tableId(ordering.getCustomerTable().getCustomerTableId())
                .totalPrice(ordering.getTotalPrice())
                .orderStatus(ordering.getOrderStatus())
                .createAt(ordering.getCreateTimeAt())
                .orderingDetailInfos(details)
                .build();
    }

    public static OrderingDetailInfo toDetailInfo(OrderingDetail detail) {
//        옵션 이름만 추출
        List<String> optionName = detail.getOrderingDetailOptions().stream().map(OrderingDetailOption::getOrderingOptionName).toList();
        return OrderingDetailInfo.builder()
                .menuName(detail.getMenu().getMenuName())
                .quantity((long) detail.getOrderingDetailQuantity())
                .option(optionName)
                .build();
    }
}
