//package com.beyond.MyoungJin.ordering.dtos;
//
//import com.beyond.MyoungJin.ordering.domain.OrderStatus;
//import com.beyond.MyoungJin.ordering.domain.Ordering;
//import com.beyond.MyoungJin.pay.domain.PaymentState;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.time.LocalDateTime;
//import java.util.UUID;
//
//@AllArgsConstructor
//@NoArgsConstructor
//@Data
//@Builder
//public class CancelledOrderResDto {
//    private Long orderingId;
//    private UUID groupId; // UUID 이대로 사용?
//    private PaymentState paymentState;
//    private OrderStatus orderStatus;
//    private Long tableId;
//    private Long totalPrice;
//
//
////    private LocalDateTime orderedAt;
////    private LocalDateTime cancelledAt;
//
//    public static CancelledOrderResDto fromEntity(Ordering ordering) {
//        return CancelledOrderResDto.builder()
//                .orderingId(ordering.getId())
//                .totalPrice(ordering.getOrderDetail().stream().mapToLong(d ->d.getMenuPrice()*d.getOrderingDetailQuantity()).sum())
//                .paymentState(ordering.getPaymentState())
//                .orderStatus(ordering.getOrderStatus())
//                .tableId(ordering.getCustomerTable().getCustomerTableId())
//                .build();
//    }
//
//
//}
