package com.beyond.pochaon.cart.service;


import com.beyond.pochaon.cart.domain.RedisCartItem;
import com.beyond.pochaon.cart.dto.cart_dto.*;
import com.beyond.pochaon.menu.domain.Menu;
import com.beyond.pochaon.menu.domain.MenuOption;
import com.beyond.pochaon.menu.domain.MenuOptionDetail;
import com.beyond.pochaon.menu.repository.MenuOptionDetailRepository;
import com.beyond.pochaon.menu.repository.MenuOptionRepository;
import com.beyond.pochaon.menu.repository.MenuRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class CartService {


    private final RedisTemplate<String, RedisCartItem> redisTemplate;
    private static final String CART_PREFIX = "cart:";
    private final MenuRepository menuRepository;
    private final MenuOptionRepository menuOptionRepository;
    private final MenuOptionDetailRepository menuOptionDetailRepository;

    public CartService(@Qualifier("cartInventory")RedisTemplate<String, RedisCartItem> redisTemplate, MenuRepository menuRepository, MenuOptionRepository menuOptionRepository, MenuOptionDetailRepository menuOptionDetailRepository) {
        this.redisTemplate = redisTemplate;
        this.menuRepository = menuRepository;
        this.menuOptionRepository = menuOptionRepository;
        this.menuOptionDetailRepository = menuOptionDetailRepository;
    }





    //    menu|option:optionDetail|option:optionDetail,optionDetail
    //    fieldKey받아오기 공통로직
    private String createFieldKey(Long menuId, List<CartCreateDto.CreateOptionId> optionId) {

        StringBuilder sb = new StringBuilder();
        sb.append(menuId);

        if (optionId == null || optionId.isEmpty()) {
            return sb.toString();
        }

//        옵션 정렬
        List<CartCreateDto.CreateOptionId> sortedOption = new ArrayList<>(optionId);
        Collections.sort(sortedOption,new Comparator<CartCreateDto.CreateOptionId>() {
            @Override
            public int compare(CartCreateDto.CreateOptionId o1, CartCreateDto.CreateOptionId o2) {
                return o1.getOptionId().compareTo(o2.getOptionId());
            }
        });

        for (CartCreateDto.CreateOptionId group : sortedOption) {
            List<Long> detailIdList = group.getOptionDetailId();
            if (detailIdList == null || detailIdList.isEmpty()) {
                continue;
            }

//            optionDetail정렬
            List<Long> sortedDetail = new ArrayList<>(detailIdList);
            Collections.sort(sortedDetail);
            sb.append("|");
            sb.append(group.getOptionId()).append(":");

            boolean first = true;
            for (Long detailId : sortedDetail) {
                if (!first) {
                    sb.append(",");
                }
                sb.append(detailId);
                first = false;
            }
        }
        return sb.toString();
    }


    //    1. 카트 주문넣기
    public void cartCreate(CartCreateDto cartCreateDto,  Long tableId) {

        String redisKey = CART_PREFIX + tableId;
        HashOperations<String, String, RedisCartItem> hashOptions = redisTemplate.opsForHash();

        for (CartCreateDto.CartCreateDetailDto detailDto : cartCreateDto.getCreateDetailDto()) {

            Menu menu = menuRepository.findById(detailDto.getMenuId()).orElseThrow(() -> new IllegalArgumentException("메뉴가 없습니다"));
            int quantity = detailDto.getMenuQuantity();
//            수량 제한
            if (quantity < 1) quantity = 1;
            if (quantity > 99) quantity = 99;

            int menuPrice = menu.getPrice();
            Long menuId = detailDto.getMenuId();

//           가격 초기화
            int optionDetailPriceSum = 0;

//           optionDetailId List // 가격합산용
            List<Long> DetailIdList =new ArrayList<>();
//            option,optionDetail //name용
            List<RedisCartItem.CartOption>cartOptionList =new ArrayList<>();

            if (detailDto.getOptionId() != null && !detailDto.getOptionId().isEmpty()) {
                for(CartCreateDto.CreateOptionId group : detailDto.getOptionId()){
                    if(group.getOptionDetailId()!=null && !group.getOptionDetailId().isEmpty()){
                        for(Long detailId: group.getOptionDetailId()){
                            DetailIdList.add(detailId);
                        }
                    }
//                    옵션이름, 디테일 이름
                    MenuOption menuOption = menuOptionRepository.findById(group.getOptionId()).orElseThrow(()->new IllegalArgumentException("옵션이 없습니다"));
//                   메뉴에 해당하는  옵션인지 검증
                    if (!menuOption.getMenu().getId().equals(menuId)) {
                        throw new IllegalArgumentException("해당 메뉴의 옵션이 아닙니다.");
                    }
                    List<String> detailNameList =new ArrayList<>();
                    if(group.getOptionDetailId()!=null && !group.getOptionDetailId().isEmpty()){
                        for(Long detailId : group.getOptionDetailId()){
                            MenuOptionDetail detail =menuOptionDetailRepository.findById(detailId).orElseThrow(()->new IllegalArgumentException("옵션디테일이 없습니다"));
//                            옵션에 해당하는 디테일인지 검증
                            if (!detail.getMenuOption().getId().equals(menuOption.getId())) {
                                throw new IllegalArgumentException("해당 옵션의 옵션디테일이 아닙니다.");
                            }
                            detailNameList.add(detail.getOptionDetailName());
                        }
                    }
//                    create->rediscart
                    cartOptionList.add(
                            RedisCartItem.CartOption.builder()
                                    .optionName(menuOption.getOptionName())
                                    .optionDetailNameList(detailNameList)
                                    .build()
                    );
                }

//                optionDetaiList 가격 합산
                if(!DetailIdList.isEmpty()){
                    optionDetailPriceSum =menuOptionDetailRepository.sumPriceByOptionDetailId(DetailIdList);
                }
            }

            int unitPrice = menuPrice + optionDetailPriceSum;


//          fieldKey생성 (menu|option:optionDetail|option:optionDetail,optionDetail) //3분할

            String fieldKey = createFieldKey(menuId,detailDto.getOptionId());
//            fieldKey에 |가 있을 때만 파싱 (option이있을때만)
            String optionKey = null;
            if(fieldKey.contains("|")) {
                optionKey = fieldKey.split("\\|", 2)[1];
            }
//            redis에서 기존 값과 겹치는 것이 있는지 조회 후 없으면 저장, //create->redis
            RedisCartItem checkItem = hashOptions.get(redisKey, fieldKey);
            if (checkItem == null) {
                RedisCartItem saveItem = RedisCartItem.builder()
                        .menuId(menuId)
                        .menuName(menu.getMenuName())
                        .optionKey(optionKey)
                        .cartOptionDtoList(cartOptionList)
                        .quantity(quantity)
                        .unitPrice(unitPrice)
                        .build();
                hashOptions.put(redisKey, fieldKey, saveItem);
//                있다면 수량 증가
            } else {
                checkItem.setQuantity(checkItem.getQuantity() + detailDto.getMenuQuantity());
                hashOptions.put(redisKey, fieldKey, checkItem);

            }
        }
    }


    //    2. 카트 조회

    public CartDto cartAll(Long tableId) {

        String redisKey = CART_PREFIX + tableId;
//      redis hash접근객체 생성
        HashOperations<String, String, RedisCartItem> hashOps = redisTemplate.opsForHash();
//        해당 장바구니 모든 항목 조회(빈카트 예외X)
        Map<String, RedisCartItem> cartItemMap = hashOps.entries(redisKey);


//        장바구니 상세목록
        List<CartDto.CartDetailDto> cartDetailDtoList = new ArrayList<>();


        int cartTotalPrice = 0;

//            redis저장된 카트데이터
        for (RedisCartItem item : cartItemMap.values()) {

//            CartOptionDto조립 Name꺼내기
            List<CartDto.CartOptionDto> optionDtoList = new ArrayList<>();

            if (item.getCartOptionDtoList() != null) {
                for (RedisCartItem.CartOption option : item.getCartOptionDtoList()) {
                    optionDtoList.add(
                            CartDto.CartOptionDto.builder()
                                    .optionName(option.getOptionName())
                                    .optionDetailNameList(option.getOptionDetailNameList())
                                    .build()
                    );
                }
            }

//            라인가격
            int lineTotalPrice= item.getUnitPrice()*item.getQuantity();


//                 CartDetailDto 조립
            CartDto.CartDetailDto detailDto = CartDto.CartDetailDto.builder()
                    .menuId(item.getMenuId())
                    .menuName(item.getMenuName())
                    .lineTotalPrice(lineTotalPrice)
                    .menuQuantity(item.getQuantity())
                    .cartOptionDtoList(optionDtoList) //옵셔 dto리스트
                    .build();

            cartDetailDtoList.add(detailDto);
//            총가격
            cartTotalPrice += lineTotalPrice;
        }
//        CartDto생성
        return CartDto.builder()
                .cartDetailDto(cartDetailDtoList)
                .CartTotalPrice(cartTotalPrice)
                .build();

    }




    //    3. 카트 수정(수량변경)
    public void UpdateQuantity(CartUpdateDto updateDto,Long tableId){

        int delta = updateDto.getDelta();

        String redisKey = CART_PREFIX + tableId;
        HashOperations<String, String,RedisCartItem>hashops = redisTemplate.opsForHash();

//      fieldKey 재사용
        String fieldKey= updateDto.getFieldKey();

//      예외
        RedisCartItem item =hashops.get(redisKey,fieldKey);
        if(item == null){
            throw new IllegalArgumentException("카트에 해당줄이 없습니다");
        }
//        수량계산
        int newQuantity =item.getQuantity()+delta;

        if(newQuantity<1){ //수량 1미만으로 조절 불가
            return;
        }

        item.setQuantity(newQuantity);

//        변경된 줄 redis에 저장
        hashops.put(redisKey,fieldKey,item);


    }


    //    4. 특정 줄 삭제
    public void LineDelete(CartLineDeleteDto lineDeleteDto,Long tableId){

        String redisKey =CART_PREFIX+tableId;
        String fieldKey = lineDeleteDto.getFieldKey();
        redisTemplate.opsForHash().delete(redisKey,fieldKey);
    }

    //    5. 카트 비우기
    public void CartClear(Long tableId){

        String redisKey = CART_PREFIX+tableId;
        redisTemplate.delete(redisKey);
    }


    //    6. 주문전표 조회용
    public List<RedisCartItem> cartItems(Long tableId){

        String redisKey = CART_PREFIX + tableId;

        HashOperations<String, String, RedisCartItem> hashOps = redisTemplate.opsForHash();
//      field(id),redis(RedisCartItem) 추출
        Map<String,RedisCartItem>entries = hashOps.entries(redisKey);

//        장바구니가 비어있으면 빈리스트 반환
        if(entries == null || entries.isEmpty()){
            return List.of();
        }

        List<RedisCartItem>cartItems =new ArrayList<>();
        for(RedisCartItem value: entries.values()){
            cartItems.add(value);
        }
        return cartItems;
    }

}






