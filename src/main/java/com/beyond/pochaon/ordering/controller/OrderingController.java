package com.beyond.pochaon.ordering.controller;


import com.beyond.pochaon.ordering.dto.OrderQueueDto;
import com.beyond.pochaon.ordering.service.OrderingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ordering")
public class OrderingController {

    private final OrderingService orderingService;

    @Autowired
    public OrderingController(OrderingService orderingService) {
        this.orderingService = orderingService;
    }

//    @GetMapping("/cancelled")
////    @PreAuthorize("hasRole('ADMIN')")   //점주만 확인할수있음
//    public List<CancelledOrderResDto> cancelledList() {
//
//        return orderingService.cancelledList()
//                .stream()
//                .map(CancelledOrderResDto::fromEntity)
//                .toList();
//    }

    //    점주 화면 로드시 standBy 주문 조회
    @GetMapping("/queue")
    public ResponseEntity<List<OrderQueueDto>> getOrderQueue(@RequestAttribute Long storeId) {
        List<OrderQueueDto> queueDtos = orderingService.getOrderQueue(storeId);
        return ResponseEntity.ok(queueDtos);
    }

    //    점주가 조리완료 버튼 클릭 -> 하단 주문내역에서 사라짐
    @PostMapping("/{orderingId}/done")
    public ResponseEntity<OrderQueueDto> completeOrder(@PathVariable Long orderingId, @RequestAttribute Long storeId) {
        OrderQueueDto dto = orderingService.completeOrder(orderingId, storeId);
        return ResponseEntity.ok(dto);
    }





}
