package com.beyond.pochaon.customerorder.controller;


import com.beyond.pochaon.customerorder.dto.OrderCreateDto;
import com.beyond.pochaon.customerorder.dto.OrderListDto;
import com.beyond.pochaon.customerorder.sercvice.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    //    1.주문 생성
    @PostMapping("/create")
    public UUID create(@RequestBody OrderCreateDto createDto, HttpServletRequest request) {
        Long tableNum = (Long)request.getAttribute("tableNum");
        Long tableId = (Long)request.getAttribute("tableId");
        Long storeId = (Long)request.getAttribute("storeId");
        if(tableNum == null || tableId ==null||storeId ==null){
            throw new IllegalArgumentException("토큰이 필요합니다");
        }

        UUID groupId = orderService.create(createDto,tableNum,tableId,storeId);
        return groupId;

    }

    //    2.추가 주문
    @PostMapping("/add")
    public UUID add(@RequestBody OrderCreateDto createDto, @RequestParam UUID groupId,HttpServletRequest request) {
        Long tableNum = (Long)request.getAttribute("tableNum");
        Long tableId = (Long)request.getAttribute("tableId");
        Long storeId = (Long)request.getAttribute("storeId");
        if(tableNum == null || tableId ==null||storeId ==null){
            throw new IllegalArgumentException("토큰이 필요합니다");
        }
        UUID existGroupId = orderService.add(createDto,groupId,tableNum,tableId,storeId);
        return existGroupId;
    }


//    3. 주문내역조회
    @GetMapping("/list")
    public List<OrderListDto> list(@RequestParam UUID groupId,HttpServletRequest request){
        Long tableNum = (Long)request.getAttribute("tableNum");
        if(tableNum == null){
            throw new IllegalArgumentException("토큰이 필요합니다");
        }
    List<OrderListDto> listDto = orderService.list(groupId,tableNum);
    return listDto;
    }

}
