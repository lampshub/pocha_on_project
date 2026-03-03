package com.beyond.pochaon.ingredient.service;

import com.beyond.pochaon.ingredient.domain.Ingredient;
import com.beyond.pochaon.ingredient.domain.IngredientDetail;
import com.beyond.pochaon.ingredient.domain.IngredientLoss;
import com.beyond.pochaon.ingredient.domain.IngredientMenu;
import com.beyond.pochaon.ingredient.dtos.IngredientMenuSaveReqDto;
import com.beyond.pochaon.ingredient.dtos.IngredientSaveReqDto;
import com.beyond.pochaon.ingredient.dtos.IngredientUsageDto;
import com.beyond.pochaon.ingredient.kafka.event.OrderEvent;
import com.beyond.pochaon.ingredient.kafka.event.QuantityAlertEvent;
import com.beyond.pochaon.ingredient.kafka.event.QuantityDecreaseFailedEvent;
import com.beyond.pochaon.ingredient.repository.IngredientDetailRepository;
import com.beyond.pochaon.ingredient.repository.IngredientLossRepository;
import com.beyond.pochaon.ingredient.repository.IngredientMenuRepository;
import com.beyond.pochaon.ingredient.repository.IngredientRepository;
import com.beyond.pochaon.menu.domain.Menu;
import com.beyond.pochaon.menu.repository.MenuRepository;
import com.beyond.pochaon.store.domain.Store;
import com.beyond.pochaon.store.repository.StoreRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@Transactional
public class IngredientService {
    private final StoreRepository storeRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientDetailRepository detailRepository;
    private final MenuRepository menuRepository;
    private final IngredientMenuRepository ingredientMenuRepository;
    private final KafkaTemplate<String, String> kafkaTemplate; // 알림도 String으로 전송
    private final ObjectMapper objectMapper;
    private final IngredientLossRepository ingredientLossRepository;

    public IngredientService(IngredientRepository ingredientRepository, StoreRepository storeRepository, IngredientDetailRepository detailRepository, MenuRepository menuRepository, IngredientMenuRepository ingredientMenuRepository, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, IngredientLossRepository ingredientLossRepository) {
        this.ingredientRepository = ingredientRepository;
        this.storeRepository = storeRepository;
        this.detailRepository = detailRepository;
        this.menuRepository = menuRepository;
        this.ingredientMenuRepository = ingredientMenuRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.ingredientLossRepository = ingredientLossRepository;
    }

    //    식자재 등록 / 입고
    public void ingredientSave(Long storeId, IngredientSaveReqDto dto) {
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new EntityNotFoundException("매장을 찾을 수 없습니다"));

        Ingredient ingredient = ingredientRepository.findByStoreIdAndNameWithLock(storeId, dto.getName())
                .orElseGet(() -> {
//                     처음 등록하는 자재라면 메타 정보 생성
                    Ingredient newIngredient = Ingredient.builder()
                            .store(store)
                            .name(dto.getName())
                            .type(dto.getType())
                            .safetyStock(dto.getSafetyStock())
                            .build();
                    return ingredientRepository.save(newIngredient);
                });

//         단가 계산
        int unitPrice = dto.getTotalPrice() / dto.getQuantity();

//         상세 입고 정보 생성
        IngredientDetail detail = IngredientDetail.builder()
                .ingredient(ingredient)
                .initialQuantity(dto.getQuantity()) // 이번에 들어온 총량
                .currentQuantity(dto.getQuantity()) // 현재 남은 수량 (주문 시 차감됨)
                .unitPrice(unitPrice)
                .totalPrice(dto.getTotalPrice())
                .deadline(dto.getDeadline())
                .build();

        detailRepository.save(detail);
    }

    //        레시피 등록(메뉴별 식자재 사용량)
    public void recipeSave(Long storeId, IngredientMenuSaveReqDto dto) {
        Menu menu = menuRepository.findById(dto.getMenuId())
                .orElseThrow(() -> new EntityNotFoundException("메뉴를 찾을 수 없습니다."));

//         레시피 등록 시 기존 레시피가 있다면 삭제 후 새로 저장
        ingredientMenuRepository.deleteByMenu(menu);

        for (IngredientUsageDto usageDto : dto.getIngredientUsageList()) {

//             식자재 조회
            Ingredient ingredient = ingredientRepository.findById(usageDto.getIngredientId())
                    .orElseThrow(() -> new EntityNotFoundException("식자재를 찾을 수 없습니다. ID: " + usageDto.getIngredientId()));

//             토큰의 storeId와 식자재의 storeId가 일치하는지 확인 (남의 매장 자재 등록 방지)
            if (!ingredient.getStore().getId().equals(storeId)) {
                throw new RuntimeException("본인 매장의 식자재만 레시피에 등록할 수 있습니다.");
            }

//             IngredientMenu객체 생성 및 저장
            IngredientMenu ingredientMenu = IngredientMenu.builder()
                    .menu(menu)
                    .ingredient(ingredient)
                    .usageAmount(usageDto.getUsageAmount())
                    .build();

            ingredientMenuRepository.save(ingredientMenu);
        }
    }

    //    안전재고 확인
    public void checkSafetyStock(Ingredient ingredient) throws JsonProcessingException {
        Integer totalCurrent = detailRepository.sumCurrentQuantityByIngredient(ingredient);
        if (totalCurrent == null) totalCurrent = 0;

        if (totalCurrent <= ingredient.getSafetyStock()) {
            QuantityAlertEvent alert = QuantityAlertEvent.builder()
                    .ingredientId(ingredient.getId())
                    .ingredientName(ingredient.getName())
                    .currentStock(totalCurrent)
                    .safetyStock(ingredient.getSafetyStock())
                    .message("안전재고 미달! 발주가 필요합니다.")
                    .build();

            String alertJson = objectMapper.writeValueAsString(alert);
            kafkaTemplate.send("stock-low-alert", alertJson);
        }
    }

    //    선입선출로 재고 감소
    public void decreaseIngredientQuantity(OrderEvent event) throws JsonProcessingException {
        List<IngredientMenu> recipes = ingredientMenuRepository.findByMenuId(event.getMenuId());

        if (recipes.isEmpty()) {
            publishFailureEvent(event, "레시피 미등록 메뉴");
            throw new RuntimeException("해당 메뉴에 등록된 레시피가 없습니다.");
        }
        for (IngredientMenu recipe : recipes) {
            Ingredient ingredient = recipe.getIngredient();

            // 총 필요량 = 메뉴 1개당 소모량 * 주문 수량
            double totalNeeded = recipe.getUsageAmount() * event.getQuantity();

            // 2. FIFO: 유통기한 순으로 가용 재고 조회
            List<IngredientDetail> activeDetails = detailRepository
                    .findAllByIngredientAndCurrentQuantityGreaterThanOrderByDeadlineAsc(ingredient, 0);

            double remaining = totalNeeded;
            for (IngredientDetail detail : activeDetails) {
                if (remaining <= 0) break;

                int batchQty = detail.getCurrentQuantity();

                if (batchQty >= remaining) {
                    // [중요] '현재 수량 - 차감할 양'을 저장해야 합니다.
                    int decreaseAmount = (int) Math.ceil(remaining);
                    detail.updateCurrentQuantity(batchQty - decreaseAmount);
                    remaining = 0;
                } else {
                    // 현재 배치가 필요한 양보다 적다면, 전량 소모(0)하고 다음 배차로 이동
                    remaining -= batchQty;
                    detail.updateCurrentQuantity(0);
                }
            }
        }
    }

    //    유통기한 만료 재료 처리
//     1. 잔량 있으면 -> ingredientLoss에 잔량 처리
//    2. IngreDetail 하드 딜리트 (데이터 개수가 많아질까봐 소프트 딜리트 대신 하드딜리트 선택 )
//    3. 스케쥴러로 사용허면 좋을 거같음
    public int cleanupExpiredIngredients(Long storeId) {
        LocalDateTime now = LocalDateTime.now();
        List<IngredientDetail> expired = detailRepository.findExpiredWithStock(storeId, now);
        int count = 0;
        for (IngredientDetail d : expired) {
//            잔량있으면 로스 기록
            if (d.getCurrentQuantity() > 0) {
                IngredientLoss loss = IngredientLoss.builder()
                        .store(d.getIngredient().getStore())
                        .ingredientName(d.getIngredient().getName())
                        .lostQuantity(d.getCurrentQuantity())
                        .unitPrice(d.getUnitPrice())
                        .lossAmount(d.getUnitPrice() * d.getCurrentQuantity())
                        .deadline(d.getDeadline())
                        .build();
                ingredientLossRepository.save(loss);
            }
//            하드 딜리트
            detailRepository.delete(d);
            count++;
        }
//        잔량 0인 만료 재료도 정리
        List<IngredientDetail> emptyExpired = detailRepository.findExpiredEmpty(storeId, now);
        for (IngredientDetail d : emptyExpired) {
            detailRepository.delete(d);
            count++;
        }
        if (count > 0) {
            log.info("매장 {}- 만료 재료 {}건 정리 완료", storeId, count);
        }
        return count;
    }

    // 실패 이벤트 발행 공통 메서드
    private void publishFailureEvent(OrderEvent event, String reason) throws JsonProcessingException {
        QuantityDecreaseFailedEvent failEvent = QuantityDecreaseFailedEvent.builder()
                .orderingId(event.getOrderingId())
                .menuId(event.getMenuId())
                .reason(reason)
                .build();

        kafkaTemplate.send("stock-decrease-failed", objectMapper.writeValueAsString(failEvent));
    }
}