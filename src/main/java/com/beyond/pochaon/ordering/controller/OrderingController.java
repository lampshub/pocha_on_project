package com.beyond.pochaon.ordering.controller;


import com.beyond.pochaon.ordering.dto.MenuDoneDto;
import com.beyond.pochaon.ordering.dto.OrderQueueDto;
import com.beyond.pochaon.ordering.service.OrderingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ordering")
public class OrderingController {

    private final OrderingService orderingService;

    @Autowired
    public OrderingController(OrderingService orderingService) {
        this.orderingService = orderingService;
    }

    //     화면 로드시 standBy 주문 조회
    @GetMapping("/queue")
    public ResponseEntity<List<OrderQueueDto>> getOrderQueue(@RequestAttribute Long storeId) {
        List<OrderQueueDto> queueDto = orderingService.getOrderQueue(storeId);
        return ResponseEntity.ok(queueDto);
    }

    //  주방에서 조리완료 버튼 클릭 -> 점주que로 들어감
    @PostMapping("/menudone")
    public ResponseEntity<?> menuDone(@RequestBody MenuDoneDto dto, @RequestAttribute Long storeId){
        orderingService.menuDone(dto, storeId);
        return ResponseEntity.ok(dto);
    }


    //    점주가 서빙완료 버튼 클릭 -> 하단 주문내역에서 사라짐
    @PostMapping("/done/{orderingId}")
    public ResponseEntity<OrderQueueDto> completeOrder(@PathVariable Long orderingId, @RequestAttribute Long storeId) {
        OrderQueueDto dto = orderingService.completeOrder(orderingId, storeId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/total")
    public ResponseEntity<?> getGroupIdTotal(@RequestParam UUID groupId) {
        return ResponseEntity.ok(orderingService.getGroupIdTotal(groupId));
    }


}
