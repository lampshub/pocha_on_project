package com.beyond.pochaon.present.service;


import com.beyond.pochaon.common.web.WebPublisher;
import com.beyond.pochaon.customerTable.domain.CustomerTable;
import com.beyond.pochaon.customerTable.repository.CustomerTableRepository;
import com.beyond.pochaon.menu.domain.Menu;
import com.beyond.pochaon.menu.repository.MenuRepository;
import com.beyond.pochaon.ordering.domain.Ordering;
import com.beyond.pochaon.ordering.domain.OrderingDetail;
import com.beyond.pochaon.ordering.repository.OrderingRepository;
import com.beyond.pochaon.present.dto.OwnerEventDto;
import com.beyond.pochaon.present.dto.PresentCreateDto;
import com.beyond.pochaon.present.dto.PresentOwnerDto;
import com.beyond.pochaon.present.dto.PresentReceiverDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PresentService {

    @Qualifier("idempotencyRedisTemplate")
    private final RedisTemplate<String, String> idempotencyRedisTemplate;
    private final OrderingRepository orderingRepository;
    private final CustomerTableRepository customerTableRepository;
    private final MenuRepository menuRepository;
    @Qualifier("groupRedisTemplate")
    private final RedisTemplate<String, String> groupRedisTemplate;
    private final WebPublisher webPublisher;

    public PresentService(@Qualifier("idempotencyRedisTemplate") RedisTemplate<String, String> idempotencyRedisTemplate, OrderingRepository orderingRepository, CustomerTableRepository customerTableRepository, MenuRepository menuRepository, RedisTemplate<String, String> groupRedisTemplate, WebPublisher webPublisher) {
        this.idempotencyRedisTemplate = idempotencyRedisTemplate;
        this.orderingRepository = orderingRepository;
        this.customerTableRepository = customerTableRepository;
        this.menuRepository = menuRepository;
        this.groupRedisTemplate = groupRedisTemplate;
        this.webPublisher = webPublisher;
    }

    //    멱등성
    private UUID idempotencyCheck(String redisKey, UUID idempotencyKey) {

//      멱등성생성(redis)
        Boolean locked = idempotencyRedisTemplate.opsForValue()
                .setIfAbsent(redisKey, "Lock", Duration.ofSeconds(3));

//        중복 시
        if (Boolean.FALSE.equals(locked)) {
            Ordering duplicated = orderingRepository.findByIdempotencyKey(idempotencyKey);
            if (duplicated != null) {
                return duplicated.getGroupId();
            }
            throw new IllegalArgumentException("이미 처리 중인 주문입니다");
        }

//      멱등성(db)
        Ordering duplicated = orderingRepository.findByIdempotencyKey(idempotencyKey);
        if (duplicated != null) {
            return duplicated.getGroupId();
        }

        return null;
    }

    public void sendPresent(PresentCreateDto createDto) {

        String redisKey = "idempotency:present:create:" + createDto.getIdempotencyKey();

//      멱등성(redis) -동시성 제어
        UUID duplicatedGroupId = idempotencyCheck(redisKey, createDto.getIdempotencyKey());

        if (duplicatedGroupId != null) {
            return;
        }

//        선물 본인테이블 제외
        if(createDto.getSenderTableNum()==(createDto.getReceiverTableNum())){
            throw new IllegalArgumentException("본인 테이블에 선물할 수 없습니다");
        }


//    테이블 조회 sender,receiver
        CustomerTable sender = customerTableRepository.findByTableNum(createDto.getSenderTableNum()).orElseThrow(() -> new IllegalArgumentException("테이블 넘버가 없습니다"));
        CustomerTable receiver = customerTableRepository.findByTableNum(createDto.getReceiverTableNum()).orElseThrow(() -> new IllegalArgumentException("테이블 넘버가 없습니다"));

//        대상 테이블 손님 여부
        String receiverGroupKey = String.valueOf(receiver.getTableNum());
        Boolean receiverActive =groupRedisTemplate.hasKey(receiverGroupKey);
        if(Boolean.FALSE.equals(receiverActive)){
            throw new IllegalArgumentException("해당 테이블에 손님이 없습니다");
        }


//    메뉴조회
        Menu menu = menuRepository.findDetailById(createDto.getMenuId()).orElseThrow(() -> new IllegalArgumentException("없는 메뉴입니다"));

//    sender기준 그룹아이디 조회
        String groupKey = String.valueOf(sender.getTableNum());
        String groupValue = groupRedisTemplate.opsForValue().get(groupKey);
        if (groupValue == null) {
            throw new IllegalArgumentException("첫주문을 먼저 진행해주세요");
        }

        UUID groupId = UUID.fromString(groupValue);

//     createDto -> db조립 (주문 조립하기)

        int totalPrice = menu.getPrice() * createDto.getMenuQuantity();

        Ordering ordering = Ordering.builder()
                .groupId(groupId)
                .customerTable(sender)
                .isPresent(true)
                .idempotencyKey(createDto.getIdempotencyKey())
                .totalPrice(totalPrice)
                .build();

        OrderingDetail detail = OrderingDetail.builder()
                .ordering(ordering)
                .menu(menu)
                .orderingDetailQuantity(createDto.getMenuQuantity())
                .menuPrice(menu.getPrice())
                .build();
        ordering.getOrderDetail().add(detail);

        orderingRepository.save(ordering);

//    점주에게 보내기 db->ownerDto

        List<PresentOwnerDto.MenuDto>menuDtoList = new ArrayList<>();

        PresentOwnerDto.MenuDto menuDto =PresentOwnerDto.MenuDto.builder()
                .menuName(menu.getMenuName())
                .menuQuantity(createDto.getMenuQuantity())
                .build();
        menuDtoList.add(menuDto);

        PresentOwnerDto ownerDto =PresentOwnerDto.builder()
                .senderTableNum(sender.getTableNum())
                .groupId(groupId)
                .receiverTableNum( receiver.getTableNum())
                .menuDtoList(menuDtoList)
                .build();

        OwnerEventDto eventDto = OwnerEventDto.builder()
                .eventType("PRESENT")
                .storeId(sender.getStore().getId())
                .payload(ownerDto)
                .build();
        webPublisher.publish(eventDto);


//    상대테이블에 보내기 db->receiverDto

        List<PresentReceiverDto.MenuDto> receiverMenuDtoList =new ArrayList<>();

        PresentReceiverDto.MenuDto receiverMenuDto = PresentReceiverDto.MenuDto.builder()
                .menuName(menu.getMenuName())
                .menuQuantity(createDto.getMenuQuantity())
                .imageUrl(menu.getMenuImageUrl())
                .build();
        receiverMenuDtoList.add(receiverMenuDto);

        PresentReceiverDto receiverDto= PresentReceiverDto.builder()
                .senderTableNum( sender.getTableNum())
                .groupId(groupId)
                .receiverTableNum( receiver.getTableNum())
                .menuList(receiverMenuDtoList)
                .build();

        webPublisher.tablePublish(receiverDto);


    }
}