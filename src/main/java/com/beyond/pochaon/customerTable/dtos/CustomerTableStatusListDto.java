package com.beyond.pochaon.customerTable.dtos;

import com.beyond.pochaon.customerTable.domain.CustomerTable;
import com.beyond.pochaon.customerTable.domain.TableStatus;
import com.beyond.pochaon.ordering.domain.OrderStatus;
import com.beyond.pochaon.ordering.domain.Ordering;
import com.beyond.pochaon.ordering.domain.OrderingDetail;
import com.beyond.pochaon.ordering.domain.OrderingDetailOption;
import com.beyond.pochaon.pay.domain.PaymentState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class                                                                                                                                                                                                                                                                CustomerTableStatusListDto {
    private Long customerTableId;
    private Long tableNum;
    private TableStatus tableStatus;
    private LocalDateTime groupCreateAt;
    private List<OrderingList> orderingList;

    public static CustomerTableStatusListDto fromEntity(CustomerTable customerTable, List<Ordering> orderingList) {
        List<OrderingList> orderingLists = orderingList.stream()
                .map(OrderingList::fromEntity)
                .toList();

        return CustomerTableStatusListDto.builder()
                .customerTableId(customerTable.getCustomerTableId())
                .tableNum(customerTable.getTableNum())
                .tableStatus(customerTable.getTableStatus())
                .groupCreateAt(customerTable.getCreateTimeAt())
                .orderingList(orderingLists)
                .build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderingList {
        private Long orderingId;
        private int totalPrice;
        private OrderStatus orderStatus;
        private PaymentState paymentState;
        private List<OrderingOption> orderingOptionList;

        public static OrderingList fromEntity(Ordering ordering) {
            List<OrderingOption> options = ordering.getOrderDetail().stream()
                    .map(OrderingOption::fromEntity)
                    .toList();

            return OrderingList.builder()
                    .orderingId(ordering.getId())
                    .totalPrice(ordering.getTotalPrice())
                    .orderStatus(ordering.getOrderStatus())
                    .paymentState(ordering.getPaymentState())
                    .orderingOptionList(options)
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderingOption {
        private String menuName;
        private Long quantity;
        private List<String> options;

        public static OrderingOption fromEntity(OrderingDetail detail) {
            List<String> optionNames = detail.getOrderingDetailOptions().stream()
                    .map(OrderingDetailOption::getOrderingOptionName)
                    .toList();

            return OrderingOption.builder()
                    .menuName(detail.getMenu().getMenuName())
                    .quantity((long) detail.getOrderingDetailQuantity())
                    .options(optionNames)
                    .build();


        }
    }
}
