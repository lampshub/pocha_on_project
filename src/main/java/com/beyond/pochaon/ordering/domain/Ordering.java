package com.beyond.pochaon.ordering.domain;

import com.beyond.pochaon.common.BaseTimeEntity;
import com.beyond.pochaon.customerTable.domain.CustomerTable;
import com.beyond.pochaon.payment.entity.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Table(name = "ordering", indexes = {
        @Index(name = "idx_ordering_table_date", columnList = "table_id, create_time_at"),
        @Index(name = "idx_ordering_table_payment_date", columnList = "table_id, payment_status, create_time_at")
})
public class Ordering extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int totalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT), nullable = false)
    private CustomerTable customerTable;

    @Column(columnDefinition = "BINARY(16)")
    private UUID groupId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.READY;

    //    멱등성 추가
    @Column(name = "idempotency_key", columnDefinition = "BINARY(16)", nullable = false, unique = true)
    private UUID idempotencyKey;

    //    주문 상태 : CANCELLED, STANDBY(주문이 왔을때) , DONE(완료 버튼 눌렀을 때)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.STANDBY;

    //    주문의 선물여부
    @Builder.Default
    @Column(nullable = false)
    private Boolean isPresent = false;

    @OneToMany(mappedBy = "ordering", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @BatchSize(size = 100)
    private List<OrderingDetail> orderDetail = new ArrayList<>();

    public void updatePaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public void updateOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }
    public void setTotalPrice(int price){
        this.totalPrice =price;
    }
}