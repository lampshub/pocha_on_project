package com.beyond.pochaon.customerTable.service;

import com.beyond.pochaon.common.auth.JwtTokenProvider;
import com.beyond.pochaon.customerTable.domain.CustomerTable;
import com.beyond.pochaon.customerTable.domain.TableStatus;
import com.beyond.pochaon.customerTable.dtos.*;
import com.beyond.pochaon.customerTable.repository.CustomerTableRepository;
import com.beyond.pochaon.ordering.domain.Ordering;
import com.beyond.pochaon.ordering.repository.OrderingRepository;
import com.beyond.pochaon.store.domain.Store;
import com.beyond.pochaon.store.repository.StoreRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CustomerTableService {
    private final CustomerTableRepository customerTableRepository;
    private final StoreRepository storeRepository;
    private final OrderingRepository orderingRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public CustomerTableService(CustomerTableRepository customerTableRepository, StoreRepository storeRepository, OrderingRepository orderingRepository, SimpMessagingTemplate simpMessagingTemplate, JwtTokenProvider jwtTokenProvider) {
        this.customerTableRepository = customerTableRepository;
        this.storeRepository = storeRepository;
        this.orderingRepository = orderingRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
    }


    //    웹소켓으로 테이블 상태 업데이트 전송
//    public void sendTableStatusUpdate(Long storeId, Long tableId) {
//        CustomerTableStatusListDto statusListDto = getTableStatus(storeId, tableId);
//        simpMessagingTemplate.convertAndSend("/topic/table-status/" + storeId, statusListDto);
//    }

    /*
매장의 전체 테이블 현황 조회 =================================

     */
    @Transactional(readOnly = true)
    public List<CustomerTableStatusListDto> customerTableStatusList(Long storeId) {
//        매장 존재 확인
        if (!storeRepository.existsById(storeId)) {
            throw new EntityNotFoundException("없는 매장입니다, ctable_ser_list");
        }

//        테이블 전체 조회
        List<CustomerTable> tables = customerTableRepository.findByStoreIdWithStore(storeId);

//       USING상태 테이블의 groupId 조회
        List<UUID> groupIds = tables.stream()
                .filter(t -> t.getTableStatus() == TableStatus.USING)
                .map(CustomerTable::getGroupId)
                .filter(Objects::nonNull)
                .toList();

//        주문 한번에 조회(List<ordering>)
        Map<UUID, List<Ordering>> orderingMap = groupIds.isEmpty()
                ? Collections.emptyMap() : orderingRepository.findAllWithDetailsByGroupIds(groupIds)
                .stream().collect(Collectors.groupingBy(Ordering::getGroupId));//같은 키값을 가진 주문을 한번에 묶음 (groupingby)

//        반환 dto
        return tables.stream().map(table -> CustomerTableStatusListDto.fromEntity(table, orderingMap.getOrDefault(table.getGroupId(), List.of()))).toList();
    }

    /*
    테이블 상세 조회
     */
    @Transactional(readOnly = true)
    public CustomerTableStatusListDto getTableStatus(Long tableId, Long storeId) {
        CustomerTable table = customerTableRepository.findById(tableId).orElseThrow(() -> new EntityNotFoundException("테이블을 찾을 수 없습니다. Ctable_ser_getTableStatus"));

        if (!table.getStore().getId().equals(storeId)) {
            throw new SecurityException("해당 테이블을 조회할 권한이 없습니다");
        }
        List<Ordering> orderingList = getOrderingsForTable(table);
        return CustomerTableStatusListDto.fromEntity(table, orderingList);
    }

    /*
    테이블 선택 후 토큰 발급
     */
    @Transactional
    public TableTokenDto selectTable(
            String email, // email 파라미터 추가
            String stage,
            Long storeId,
            TableSelectDto dto
    ) {
        CustomerTable table = customerTableRepository.findByTableNum(dto.getTableNum())
                .orElseThrow(() -> new EntityNotFoundException("없는 테이블입니다"));

        if (!"STORE".equals(stage)) {
            throw new AccessDeniedException("STORE 토큰이 필요합니다.");
        }

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("Store not found"));

        // 수정한 createTableToken 호출 (email 전달)
        String tableToken = jwtTokenProvider.createTableToken(
                email,
                storeId,
                dto.getTableNum(),
                table.getCustomerTableId()
        );

        table.setTableStatusUsing();
        return new TableTokenDto(tableToken);
    }


    private List<Ordering> getOrderingsForTable(CustomerTable table) {
        if (table.getTableStatus() != TableStatus.USING || table.getGroupId() == null) {
            return List.of();
        }
        return orderingRepository.findAllWithDetailsByGroupId(table.getGroupId());
    }

    //    채팅 가능한 테이블 목록 조회
    @Transactional(readOnly = true)
    public List<AvailableTableDto> getAvailableTables(Long storeId, Integer myTableNum) {

        List<CustomerTable> tables =
                customerTableRepository
                        .findByStoreIdAndTableStatusAndTableNumNot(
                                storeId,
                                TableStatus.USING,
                                myTableNum
                        );

        return tables.stream()
                .map(t -> AvailableTableDto.builder()
                        .tableNum(Math.toIntExact(t.getTableNum()))
                        .build())
                .toList();
    }

    // 점주 설정관리 화면에서 테이블 관리
    public void create(Long storeId, TableCreateReqDto dto) {
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new EntityNotFoundException("없는 매장/ customertable_ser-create"));
//
//        TableCreateDto dto = TableCreateDto.builder()
//                .tableNum(dtoe.getTableNum())
//                .store(store)
//                .build();
        if (customerTableRepository.existsByStoreIdAndTableNum(storeId, dto.getTableNum())) {
            throw new IllegalArgumentException("이미 존재하는 테이블 번호입니다.");
        }
        CustomerTable customerTable = CustomerTable.builder()
                .tableNum(dto.getTableNum())
                .store(store)
                .build();
        customerTableRepository.save(customerTable);
    }

    public void delete(Long storeId, Long customerTableId) {
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new EntityNotFoundException("없는 매장"));
        CustomerTable customerTable = customerTableRepository.findById(customerTableId).orElseThrow(() -> new EntityNotFoundException("없는 테이블"));
        customerTableRepository.delete(customerTable);
    }

    public List<TableResToOwnerDto> getTables(Long storeId) {
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new EntityNotFoundException("없는 매장"));
        return customerTableRepository.findByStoreId(storeId).stream()
                .map(c_table -> new TableResToOwnerDto(
                        c_table.getCustomerTableId(),
                        c_table.getTableNum()))
                .toList();
    }

    public void tableRollBack(int tableNum) {
        CustomerTable customerTable = customerTableRepository.findByTableNum(tableNum).orElseThrow(() -> new EntityNotFoundException("없는 테이블/ customerTable_ser_tableRollBack"));
        customerTable.setTableStatusStandBy();
    }
}

