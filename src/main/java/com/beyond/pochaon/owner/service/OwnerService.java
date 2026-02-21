package com.beyond.pochaon.owner.service;

import com.beyond.pochaon.owner.domain.Owner;
import com.beyond.pochaon.owner.dtos.BusinessApiReqDto;
import com.beyond.pochaon.owner.dtos.BusinessApiResDto;
import com.beyond.pochaon.owner.dtos.OwnerStoreSettlementResDto;
import com.beyond.pochaon.owner.repository.OwnerRepository;
import com.beyond.pochaon.owner.repository.OwnerVerifyClient;
import com.beyond.pochaon.store.domain.Store;
import com.beyond.pochaon.store.domain.StoreSettlement;
import com.beyond.pochaon.store.dtos.SimpleSettlementDto;
import com.beyond.pochaon.store.repository.StoreRepository;
import com.beyond.pochaon.store.repository.StoreSettlementRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestAttribute;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class OwnerService {
    private final OwnerVerifyClient ownerVerifyClient;
    private final StoreSettlementRepository storeSettlementRepository;
    private final OwnerRepository ownerRepository;
    private final StoreRepository storeRepository;

    @Autowired
    public OwnerService(OwnerVerifyClient ownerVerifyClient, StoreSettlementRepository storeSettlementRepository, OwnerRepository ownerRepository, StoreRepository storeRepository) {
        this.ownerVerifyClient = ownerVerifyClient;
        this.storeSettlementRepository = storeSettlementRepository;
        this.ownerRepository = ownerRepository;
        this.storeRepository = storeRepository;
    }

    public BusinessApiResDto verify(BusinessApiReqDto reqDto) {
        BusinessApiResDto resDto = ownerVerifyClient.verify(reqDto);

        if (resDto == null) {
            throw new IllegalStateException("사업자진위확인 API 응답이 없습니다.");
        }
        if (!resDto.getStatus_code().equals("OK")) {
            throw new IllegalStateException("사업자 확인 실패" + resDto.getMessage());
        }
        if (resDto.getData() == null || resDto.getData().isEmpty()) {
            throw new IllegalStateException("사업자 정보가 존재하지 않습니다.");
        }

        return resDto;
    }

    public SimpleSettlementDto getSettlement(Long storeId) {
        StoreSettlement settlement = storeSettlementRepository.findByStoreId(storeId);
        return SimpleSettlementDto.builder()
                .dayTotal(settlement.getDayTotalAmount())
                .orderCount(settlement.getOrderCount())
                .averageOrderAmount(settlement.getAverageOrderAmount())
                .build();
    }


    public OwnerStoreSettlementResDto getStoreSettlement(String email) {
        Owner owner = ownerRepository.findByOwnerEmail(email).orElseThrow(() -> new EntityNotFoundException("없는 점주입니다. owner_ser_getstoreSettlement"));

        List<Store> storeList = storeRepository.findByOwnerId(owner.getId());

        YearMonth current = YearMonth.now();
        LocalDateTime startDate = current.atDay(1).atStartOfDay();
        LocalDateTime endDate = current.plusMonths(1).atDay(1).atStartOfDay();

        List<StoreSettlement> settlements = storeSettlementRepository.findByOwnerIdAndMonth(owner.getId(), startDate, endDate);

        int storeTotalAmount = settlements.stream()
                .mapToInt(StoreSettlement::getDayTotalAmount)
                .sum();

        int storeAverageAmount = settlements.isEmpty() ? 0 : storeTotalAmount / settlements.size();


        Map<String, Integer> storeAmountMap = new HashMap<>();
//        매장별 매출 합계 구하기
        for (StoreSettlement ss : settlements) {
            String storeName = ss.getStore().getStoreName();
            int amount = ss.getDayTotalAmount();
            storeAmountMap.put(storeName, storeAmountMap.getOrDefault(storeName, 0) + amount);
        }
        String topStoreName = null;
        int topAmount = Integer.MIN_VALUE;

        for (Map.Entry<String, Integer> map : storeAmountMap.entrySet()) {
            if (map.getValue() > topAmount) {
                topAmount = map.getValue();
                topStoreName = map.getKey();
            }
        }

        return OwnerStoreSettlementResDto.builder()
                .storeCount(storeList.size())
                .topStoreName(topStoreName)
                .storeTotalAmount(storeTotalAmount)
                .storeDayAverageAmount(storeAverageAmount)
                .build();
    }

}
