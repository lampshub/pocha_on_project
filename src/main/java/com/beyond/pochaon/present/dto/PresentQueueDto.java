package com.beyond.pochaon.present.dto;

import com.beyond.pochaon.ordering.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresentQueueDto {
    private Long orderingId; // 주문 ID
    private String type = "PRESENT";
    private Long senderTableId; // 발신 테이블 번호
    private Long receiverTableId; // 수신 테이블 번호
    private int receiverTableNum; // 수신 테이블 번호
    private int totalPrice; // 주문 총액
    private OrderStatus orderStatus; //주문 상태
    private List<PresentDetail> orderingDetailInfos; //주문 상품 리스트

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PresentDetail {
        private String menuName; //메뉴명
        private int quantity; //수량
    }
}
