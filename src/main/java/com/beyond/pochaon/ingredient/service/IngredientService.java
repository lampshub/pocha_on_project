package com.beyond.pochaon.ingredient.service;

import com.beyond.pochaon.ingredient.domain.*;
import com.beyond.pochaon.ingredient.dtos.*;
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
    private final KafkaTemplate<String, Object> kafkaTemplate; // 알림도 String으로 전송
    private final ObjectMapper objectMapper;
    private final IngredientLossRepository ingredientLossRepository;

    public IngredientService(IngredientRepository ingredientRepository, StoreRepository storeRepository, IngredientDetailRepository detailRepository, MenuRepository menuRepository, IngredientMenuRepository ingredientMenuRepository, KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper, IngredientLossRepository ingredientLossRepository) {
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
                            .unit(dto.getUnit() != null ? dto.getUnit() : "g")
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
            return; // 레시피 미등록 메뉴는 재고 차감 스킵 (checkStockAvailability와 동일)
        }
        for (IngredientMenu recipe : recipes) {
            Ingredient ingredient = recipe.getIngredient();
            double totalNeeded = recipe.getUsageAmount() * event.getQuantity();

            List<IngredientDetail> activeDetails = detailRepository
                    .findAllByIngredientAndCurrentQuantityGreaterThanOrderByDeadlineAsc(ingredient, 0);

            double remaining = totalNeeded;
            for (IngredientDetail detail : activeDetails) {
                if (remaining <= 0) break;
                int batchQty = detail.getCurrentQuantity();
                if (batchQty >= remaining) {
                    int decreaseAmount = (int) Math.ceil(remaining);
                    detail.updateCurrentQuantity(batchQty - decreaseAmount);
                    remaining = 0;
                } else {
                    remaining -= batchQty;
                    detail.updateCurrentQuantity(0);
                }
            }

            //  재고 부족 시 실패 이벤트 발행 + 예외
            if (remaining > 0) {
                String reason = String.format("재고 부족: %s (필요: %.1f, 부족: %.1f)",
                        ingredient.getName(), totalNeeded, remaining);
                publishFailureEvent(event, reason);
                throw new RuntimeException(reason);
            }
        }
    }

//    유통기한 만료 재료 처리
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
                        .ingredientId(d.getIngredient().getId())
                        .reason("유통기한 만료")
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

//    모든 매장의 유통기한 지난 식자재 처리
    public void cleanupExpiredIngredientsForAll() {
        List<Store> stores = storeRepository.findAll();
        for (Store store : stores) {
            cleanupExpiredIngredients(store.getId());
        }
    }

    //  전체 식자재 리스트 조회 (상태 배지 포함)
    @Transactional(readOnly = true)
    public List<IngredientListResDto> getIngredientList(Long storeId) {
        List<Ingredient> ingredients = ingredientRepository.findByStoreId(storeId);

        return ingredients.stream().map(ing -> {
            // 해당 식자재의 전체 가용 재고 합산
            Integer totalCurrent = detailRepository.sumCurrentQuantityByIngredient(ing);
            int current = (totalCurrent != null) ? totalCurrent : 0;
            int safety = ing.getSafetyStock();

            // 배지 색상 결정 로직
            StockStatus status = calculateStockStatus(current, ing.getSafetyStock());

            return IngredientListResDto.builder()
                    .ingredientId(ing.getId())
                    .name(ing.getName())
                    .type(ing.getType())
                    .currentStock(current)
                    .safetyStock(ing.getSafetyStock())
                    .status(status) // Enum 주입
                    .unit(ing.getUnit())
                    .build();
        }).toList();
    }

    //  특정 메뉴의 레시피 리스트 조회
    @Transactional(readOnly = true)
    public RecipeDetailResDto getRecipeByMenu(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new EntityNotFoundException("메뉴를 찾을 수 없습니다."));

        List<IngredientMenu> recipes = ingredientMenuRepository.findByMenuId(menuId);

        List<RecipeDetailResDto.IngredientUsageInfo> ingredientInfos = recipes.stream()
                .map(r -> RecipeDetailResDto.IngredientUsageInfo.builder()
                        .ingredientId(r.getIngredient().getId())
                        .ingredientName(r.getIngredient().getName())
                        .usageAmount(r.getUsageAmount())
                        .unit(r.getIngredient().getUnit())
                        .build())
                .toList();

        return RecipeDetailResDto.builder()
                .menuId(menu.getId())
                .menuName(menu.getMenuName())
                .ingredients(ingredientInfos)
                .build();
    }

    // 배지 색상 계산 내부 메서드
    private StockStatus calculateStockStatus(int current, int safety) {
        if (safety <= 0) return StockStatus.GREEN;

        double ratio = (double) current / safety;

//        0~50% 남았으면 빨간색 알림
        if (ratio < 0.5) {
            return StockStatus.RED;
        }
//        50~100% 남았으면 노란색 알림
        else if (ratio <= 1.0) {
            return StockStatus.YELLOW;
        }
//        그 이상이면 초록색 알림
        else {
            return StockStatus.GREEN;
        }
    }

    //  식자재 메타 정보 수정
    public void updateIngredient(Long ingredientId, IngredientUpdateReqDto dto) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new EntityNotFoundException("식자재를 찾을 수 없습니다."));

        // 값이 들어온 경우에만 업데이트
        if (dto.getName() != null) ingredient.modifyName(dto.getName());
        if (dto.getType() != null) ingredient.modifyType(dto.getType());
        if (dto.getSafetyStock() != null) ingredient.modifySafetyStock(dto.getSafetyStock());
        if (dto.getUnit() != null) ingredient.modifyUnit(dto.getUnit());
    }

    //  식자재 삭제
    public void deleteIngredient(Long ingredientId) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new EntityNotFoundException("식자재를 찾을 수 없습니다."));

        // 레시피에 등록되어 있다면 삭제 불가하게 처리하거나, 레시피부터 삭제 유도
        if (ingredientMenuRepository.existsByIngredient(ingredient)) {
            throw new RuntimeException("레시피에 등록된 식자재는 삭제할 수 없습니다. 레시피를 먼저 수정하세요.");
        }

        ingredientRepository.delete(ingredient);
    }

    //  실재고 조정
    public void adjustStock(Long ingredientId, StockAdjustReqDto dto) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new EntityNotFoundException("식자재를 찾을 수 없습니다."));

        // 1. 현재 전산상의 총 재고 합계 조회
        Integer currentTotal = detailRepository.sumCurrentQuantityByIngredient(ingredient);
        int total = (currentTotal != null) ? currentTotal : 0;

        // 2. 차이 계산 (전산 - 실제)
        int diff = total - dto.getActualQuantity();

        if (diff > 0) {
            //  재고가 부족한 경우 (분실/파손 등) -> FIFO 차감 및 Loss 기록
            adjustDecrease(ingredient, diff, dto.getReason());
        } else if (diff < 0) {
            //  재고가 더 많은 경우 (입고 누락 등) -> 가장 최근 배차에 추가
            adjustIncrease(ingredient, Math.abs(diff));
        }

        log.info("재고 조정 완료: {} (전산: {} -> 실제: {})", ingredient.getName(), total, dto.getActualQuantity());
    }

    private void adjustDecrease(Ingredient ingredient, int amountToDecrease, String reason) {
        // 유통기한 순으로 가용 재고 조회
        List<IngredientDetail> activeDetails = detailRepository
                .findAllByIngredientAndCurrentQuantityGreaterThanOrderByDeadlineAsc(ingredient, 0);

        int remaining = amountToDecrease;
        for (IngredientDetail detail : activeDetails) {
            if (remaining <= 0) break;

            int currentQty = detail.getCurrentQuantity();
            int take = Math.min(currentQty, remaining);

            // 상세 재고 차감
            detail.updateCurrentQuantity(currentQty - take);

            // Loss(손실) 테이블에 기록
            IngredientLoss loss = IngredientLoss.builder()
                    .store(ingredient.getStore())
                    .ingredientName(ingredient.getName())
                    .lostQuantity(take)
                    .unitPrice(detail.getUnitPrice())
                    .lossAmount(take * detail.getUnitPrice())
                    .deadline(detail.getDeadline())
                    .reason("수동 조정: " + reason) // 사유 기록
                    .build();
            ingredientLossRepository.save(loss);

            remaining -= take;
        }
    }

//    매일 자정 스케줄러 -> 유통기한 지난 식자재 Loss 테이블에 기록
    private void adjustIncrease(Ingredient ingredient, int amountToIncrease) {
        // 가장 유통기한이 넉넉한(마지막) 배차를 찾아 수량 추가
        // 만약 배차가 하나도 없다면 새로 생성하거나 예외 처리가 필요할 수 있습니다.
        List<IngredientDetail> details = detailRepository
                .findAllByIngredientAndCurrentQuantityGreaterThanOrderByDeadlineDesc(ingredient, 0);

        if (!details.isEmpty()) {
            IngredientDetail latest = details.get(0);
            latest.updateCurrentQuantity(latest.getCurrentQuantity() + amountToIncrease);
        } else {
            throw new RuntimeException("증액할 기존 입고 이력이 없습니다. '입고 등록'을 이용해 주세요.");
        }
    }

//    재고 가용성 확인
    @Transactional(readOnly = true)
    public String checkStockAvailability(Long menuId, int quantity) {
        List<IngredientMenu> recipes = ingredientMenuRepository.findByMenuId(menuId);

        if (recipes.isEmpty()) {
            return null; // 레시피 미등록 메뉴는 재고 검증 스킵
        }

        for (IngredientMenu recipe : recipes) {
            Ingredient ingredient = recipe.getIngredient();
            double totalNeeded = recipe.getUsageAmount() * quantity;

            // 현재 가용 재고 합산
            Integer totalCurrent = detailRepository.sumCurrentQuantityByIngredient(ingredient);
            int available = (totalCurrent != null) ? totalCurrent : 0;

            if (available < totalNeeded) {
                return String.format("%s (필요: %.0f, 가용: %d)",
                        ingredient.getName(), totalNeeded, available);
            }
        }

        return null; // 모든 식자재 충분
    }
}