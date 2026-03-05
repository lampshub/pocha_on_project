package com.beyond.pochaon.store.service;

import com.beyond.pochaon.common.auth.JwtTokenProvider;
import com.beyond.pochaon.common.service.SmsSender;
import com.beyond.pochaon.owner.domain.Owner;
import com.beyond.pochaon.owner.domain.Role;
import com.beyond.pochaon.owner.repository.OwnerRepository;
import com.beyond.pochaon.store.domain.Store;
import com.beyond.pochaon.store.domain.StoreStatus;
import com.beyond.pochaon.store.domain.UsageStatus;
import com.beyond.pochaon.store.dto.*;
import com.beyond.pochaon.store.repository.StoreRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Transactional
public class StoreService {

    private final StoreRepository storeRepository;
    private final OwnerRepository ownerRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final SmsSender smsSender;

    @Qualifier("smsVerify")
    private final RedisTemplate<String, String> smsRedisTemplate;

    @Autowired
    public StoreService(StoreRepository storeRepository, OwnerRepository ownerRepository, JwtTokenProvider jwtTokenProvider, @Qualifier("isLogin") RedisTemplate<String, String> redisTemplate, PasswordEncoder passwordEncoder, SmsSender smsSender, @Qualifier("smsVerify")RedisTemplate<String, String> smsRedisTemplate) {
        this.storeRepository = storeRepository;
        this.ownerRepository = ownerRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = passwordEncoder;
        this.smsSender = smsSender;
        this.smsRedisTemplate = smsRedisTemplate;
    }

    public void createStore(StoreCreateDto dto, String email) {
        Owner owner = ownerRepository.findByOwnerEmail(email).orElseThrow(() -> new EntityNotFoundException("없는 이메일입니다. store_ser_create부분"));
        Store store = Store.builder()
                .storeName(dto.getStoreName())
                .address(dto.getAddress())
                .phoneNumber(dto.getPhoneNumber())
                .owner(owner)
                .serviceStartAt(dto.getServiceStartAt())
                .serviceEndAt(dto.getServiceStartAt().plusMonths(1).minusDays(1))
                .autoRenew(dto.isAutoRenew())
                .storeAccessKey(passwordEncoder.encode(dto.getStoreAccessKey()))
                .build();
        storeRepository.save(store);
    }

    @Transactional(readOnly = true)
    public List<StoreListDto> findAll(String email) {
        Owner owner = ownerRepository.findByOwnerEmail(email).orElseThrow(() -> new EntityNotFoundException("Owner not found"));
        List<Store> storeList;
        if (owner.getRole() == Role.ADMIN) {
            storeList = storeRepository.findAll();
        } else {
            storeList = storeRepository.findByOwner(owner);
        }
        return storeList.stream()
                .map(StoreListDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StoreTokenDto selectStore(String email, Long storeId, String storeAccessKey) {
        Owner owner = ownerRepository.findByOwnerEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Owner not found"));
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("Store not found"));
        // 매장 접근 키 검증
        if (!passwordEncoder.matches(storeAccessKey, store.getStoreAccessKey())) {
            throw new AccessDeniedException("매장 접근 키가 일치하지 않습니다.");
        }
        if (!store.getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("해당 매장에 대한 권한이 없습니다.");
        }
        if (store.getStatus() != StoreStatus.APPROVED){
            throw new IllegalStateException("승인되지 않은 매장입니다.");
        }
        UsageStatus usageStatus = store.getUsageStatus();
        if(usageStatus == UsageStatus.BEFORE_START){
            throw new IllegalStateException("서비스 이용 시작일 전 입니다.");
        }
        if(usageStatus == UsageStatus.EXPIRED){
            throw new IllegalStateException("서비스 이용기간 만료된 매장입니다.");
        }
        String storeAccessToken = jwtTokenProvider.createStoreAccessToken(owner, storeId);
        String key = String.valueOf(store.getId());
        redisTemplate.opsForValue().set(key, "active");
        return new StoreTokenDto(storeAccessToken);
    }

    @Transactional(readOnly = true)
    public StoreTokenDto selectStoreForSettlement(String email, Long storeId) {
        Owner owner = ownerRepository.findByOwnerEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Owner not found"));
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("Store not found"));
        if (!store.getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("해당 매장에 대한 권한이 없습니다.");
        }
        String storeAccessToken = jwtTokenProvider.createStoreAccessToken(owner, storeId);
        return new StoreTokenDto(storeAccessToken);
    }

    @Transactional(readOnly = true)
    public StoreTimeResDto getStoreHours(Long storeId) {
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new IllegalStateException("Store not found"));
        return new StoreTimeResDto(store.getStoreOpenAt(), store.getStoreCloseAt());
    }

    @Transactional
    public void updateTime(Long storeId, StoreUpdateTimeDto dto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Owner owner = ownerRepository.findByOwnerEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Owner not found"));
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("Store not found"));
        if (!store.getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("해당 매장에 대한 권한이 없습니다");
        }
        if (dto.getOpenAt().equals(dto.getCloseAt())) {
            throw new IllegalStateException("오픈시간과 마감시간은 같을 수 없습니다");
        }
        store.updateTime(dto.getOpenAt(), dto.getCloseAt());
    }

    public Map<Long, Boolean> isLogin(List<IsLoginStoreReqDto> dtoList) {
        Map<Long, Boolean> isLoginMap = new HashMap<>();
        for (IsLoginStoreReqDto d : dtoList) {
            String key = String.valueOf(d.getStoreId());
            boolean isOpen = redisTemplate.hasKey(key);
            isLoginMap.put(d.getStoreId(), isOpen);
        }
        return isLoginMap;
    }

    public void deleteKey(Long storeId) {
        if (storeRepository.findById(storeId).isEmpty()) {
            throw new EntityNotFoundException("없는 매장/ store_ser_deletekey");
        }
        redisTemplate.delete(String.valueOf(storeId));
    }



    @Transactional
    public void updateAutoRenew(Long storeId, String email, Boolean autoRenew){
        Owner owner = ownerRepository.findByOwnerEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Owner not found"));
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("Store not found"));
        if (!store.getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("해당 매장에 대한 권한이 없습니다");
        }
        store.updateAutoRenew(autoRenew);

    }

    public void sendStoreAccessKeySms(String ownerEmail, Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("매장을 찾을 수 없습니다."));
        if (!store.getOwner().getOwnerEmail().equals(ownerEmail)) {
            throw new IllegalArgumentException("본인 소유의 매장이 아닙니다.");
        }

        String phone = store.getPhoneNumber();
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("매장에 등록된 전화번호가 없습니다.");
        }

        String cooldownKey = "SMS_COOLDOWN:STORE:" + storeId;
        if (smsRedisTemplate.hasKey(cooldownKey)) {
            throw new IllegalArgumentException("1분 후에 다시 시도해주세요.");
        }

        String code = String.valueOf(100000 + new Random().nextInt(900000));
        String codeKey = "SMS_CODE:STORE:" + storeId;
        smsRedisTemplate.opsForValue().set(codeKey, code, Duration.ofMinutes(3));
        smsRedisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofMinutes(1));

        smsSender.send(phone, code);
    }

    // ── 매장 키 재설정: SMS 검증 + 새 키 저장 ───────────────
    public void resetStoreAccessKey(String ownerEmail, Long storeId, String inputCode, String newAccessKey) {
        // 1) 매장 조회 + 본인 소유 검증
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("매장을 찾을 수 없습니다."));
        if (!store.getOwner().getOwnerEmail().equals(ownerEmail)) {
            throw new IllegalArgumentException("본인 소유의 매장이 아닙니다.");
        }

        // 2) 실패 횟수 블록 체크
        String blockKey = "SMS_BLOCK:STORE:" + storeId;
        if (smsRedisTemplate.hasKey(blockKey)) {
            throw new IllegalArgumentException("인증 시도 횟수를 초과했습니다. 10분 후 다시 시도해주세요.");
        }

        // 3) Redis에서 코드 조회
        String codeKey = "SMS_CODE:STORE:" + storeId;
        String savedCode = smsRedisTemplate.opsForValue().get(codeKey);
        if (savedCode == null) {
            throw new IllegalArgumentException("인증번호가 만료되었습니다. 다시 요청해주세요.");
        }

        // 4) 코드 검증
        if (!savedCode.equals(inputCode)) {
            String failKey = "SMS_FAIL:STORE:" + storeId;
            Long failCount = smsRedisTemplate.opsForValue().increment(failKey);
            smsRedisTemplate.expire(failKey, Duration.ofMinutes(10));
            if (failCount != null && failCount >= 5) {
                smsRedisTemplate.opsForValue().set(blockKey, "1", Duration.ofMinutes(10));
                smsRedisTemplate.delete(codeKey);
            }
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다.");
        }

        // 5) 새 키 유효성 검사
        if (newAccessKey == null || newAccessKey.trim().length() < 3) {
            throw new IllegalArgumentException("매장 접근 키는 3자리 이상이어야 합니다.");
        }

        // 6) 성공 → Redis 정리 + 새 키 저장
        smsRedisTemplate.delete(codeKey);
        smsRedisTemplate.delete("SMS_FAIL:STORE:" + storeId);

        store.updateStoreAccessKey(passwordEncoder.encode(newAccessKey));
        storeRepository.save(store);
    }
}