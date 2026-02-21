package com.beyond.pochaon.customerTable.controller;

import com.beyond.pochaon.customerTable.dtos.*;
import com.beyond.pochaon.customerTable.service.CustomerTableService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customertable")
public class CustomerTableController {

    private final CustomerTableService customerTableService;

    @Autowired
    public CustomerTableController(CustomerTableService customerTableService) {
        this.customerTableService = customerTableService;
    }


    //    점주 화면기준에서 테이블 현황 json으로 받아옴 //06.02.05
    @GetMapping("/tablestatuslist") // 소문자로 변경
    public ResponseEntity<?> cTableStatusList(@RequestAttribute(value = "storeId", required = false) Long storeId) {
        // 만약 필터에서 storeId를 못찾는다면 에러 처리
        if (storeId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Store ID missing in token");
        }
        List<CustomerTableStatusListDto> customerTableStatusListDtoList = customerTableService.customerTableStatusList(storeId);
        return ResponseEntity.status(HttpStatus.OK).body(customerTableStatusListDtoList);
    }



    @GetMapping("/list")
    public ResponseEntity<?> cTableList(
            @RequestAttribute(value = "storeId", required = false) Long storeIdFromToken,
            @RequestParam(value = "storeId", required = false) Long storeIdFromParam
    ) {
        Long storeId = storeIdFromToken != null ? storeIdFromToken : storeIdFromParam;
        if (storeId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<CustomerTableStatusListDto> dtoList = customerTableService.customerTableStatusList(storeId);
        return ResponseEntity.status(HttpStatus.OK).body(dtoList);
    }

    @PostMapping("/select")
    public TableTokenDto selectTable(
            @RequestAttribute String stage,
            @RequestAttribute Long storeId,
            @RequestBody TableSelectDto dto
    ) {
        return customerTableService.selectTable(stage, storeId, dto);
    }

    @GetMapping("/available")
    public ResponseEntity<List<AvailableTableDto>> getAvailableTables(
            HttpServletRequest request
    ) {

        Long storeId = (Long) request.getAttribute("storeId");
        Integer tableNum = (Integer) request.getAttribute("tableNum");

        if (storeId == null || tableNum == null)
            throw new IllegalStateException("인증 정보 없음");

        List<AvailableTableDto> result =
                customerTableService.getAvailableTables(storeId, tableNum);

        return ResponseEntity.ok(result);
    }

    //    점주 설정관리 화면 - 테이블관리 - 추가/삭제/목록조회
    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestAttribute Long storeId,@RequestBody TableCreateReqDto dto) {
        customerTableService.create(storeId,dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("created");
    }

    @DeleteMapping("/{customertableid}")    //프론트에서는 tableNum으로 선택받음
    public ResponseEntity<?> delete(@RequestAttribute Long storeId, @PathVariable("customertableid") Long customerTableId) {
        customerTableService.delete(storeId, customerTableId);
        return ResponseEntity.status(HttpStatus.OK).body("deleted");
    }

    @GetMapping("/gettablelist")
    public ResponseEntity<?> getTables(@RequestAttribute Long storeId) {
        return ResponseEntity.status(HttpStatus.OK).body(customerTableService.getTables(storeId));
    }



    // 2. 가변 경로는 아래쪽에 배치하거나 더 구체적으로 명시
    // 예: @GetMapping("/detail/{tableId}") 로 바꾸는 것이 가장 안전합니다.
    @GetMapping("/{tableId}")
    public ResponseEntity<?> customerTableDetail(@PathVariable Long tableId, @RequestAttribute("storeId") Long storeId) {
        CustomerTableStatusListDto dto = customerTableService.getTableStatus(tableId, storeId);
        return ResponseEntity.status(HttpStatus.OK).body(dto);
    }
}



/*
[
  {
    "cTableId": 1,
    "tableStatus": "USING",
    "groupCreateAt": "2026-02-03T10:30:00",
    "orderingList": [
      {
        "orderingId": 1,
        "totalPrice": 25000,
        "orderStatus": "PENDING",
        "paymentState": "DONE",
        "orderingOptionList": [
          {
            "menuName": "짜장면",
            "quantity": 2,
            "options": ["곱빼기"]
          },
          {
            "menuName": "탕수육",
            "quantity": 1,
            "options": []
          }
        ]
      }
    ]
  },
  {
    "cTableId": 2,
    "tableStatus": "STANDBY",
    "groupCreateAt": null,
    "orderingList": []
  }
]
 */
