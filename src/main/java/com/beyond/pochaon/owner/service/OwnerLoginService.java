package com.beyond.pochaon.owner.service;

import com.beyond.pochaon.common.auth.JwtTokenProvider;
import com.beyond.pochaon.common.auth.TokenStage;
import com.beyond.pochaon.common.dto.SmsSendReqDto;
import com.beyond.pochaon.common.service.SmsSender;
import com.beyond.pochaon.customerTable.domain.CustomerTable;
import com.beyond.pochaon.customerTable.domain.TableStatus;
import com.beyond.pochaon.customerTable.repository.CustomerTableRepository;
import com.beyond.pochaon.owner.domain.Owner;
import com.beyond.pochaon.owner.dto.BaseLoginDto;
import com.beyond.pochaon.owner.dto.OwnerCreateDto;
import com.beyond.pochaon.owner.dto.TokenDto;
import com.beyond.pochaon.owner.repository.OwnerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Transactional
public class OwnerLoginService {
    private final OwnerRepository ownerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomerTableRepository customerTableRepository;
    private final SmsSender smsSender;
    @Qualifier("smsVerify")
    private final RedisTemplate<String,String> redis;

    private static final long CODE_TTL = 3;
    private static final int MAX_FAIL = 5;


    public OwnerLoginService(OwnerRepository ownerRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider, CustomerTableRepository customerTableRepository, SmsSender smsSender, @Qualifier("smsVerify") RedisTemplate<String, String> redis) {
        this.ownerRepository = ownerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.customerTableRepository = customerTableRepository;
        this.smsSender = smsSender;
        this.redis = redis;
    }

    public TokenDto baseLogin(BaseLoginDto dto) {
        Owner owner = ownerRepository.findByOwnerEmail(dto.getOwnerEmail()).orElseThrow(() -> new EntityNotFoundException("잘못된 입력입니다."));
//        비밀번호 검증
        if (!passwordEncoder.matches(dto.getPassword(), owner.getPassword())) {
            throw new EntityNotFoundException("잘못된 입력입니다.");
        }
        String name = owner.getOwnerName();
        String baseAccessToken = jwtTokenProvider.createBaseAccessToken(owner, TokenStage.BASE, null);
        String baseRefreshToken = jwtTokenProvider.createRefreshToken(owner);
        return TokenDto.fromEntity(baseAccessToken, baseRefreshToken, name);
    }

    public void ownerSave(OwnerCreateDto dto) {
        if (ownerRepository.findByOwnerEmail(dto.getOwnerEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 사용중인 이메일입니다.");
        } else if (ownerRepository.existsByBusinessRegistrationNumber(dto.getBusinessRegistrationNumber())){
            throw new IllegalArgumentException("이미 가입된 사업장입니다.");
        } else {
            ownerRepository.save(dto.toEntity(passwordEncoder.encode(dto.getPassword()), passwordEncoder.encode(dto.getSettlementKey())));
        }
    }

    public TokenDto refresh(String refreshToken) {

        // 1. RT 검증 + Owner 조회
        Owner owner = jwtTokenProvider.validateRefreshToken(refreshToken);


        // 2. 새로운 토큰 발급
        String newAccessToken =
                jwtTokenProvider.createBaseAccessToken(
                        owner,
                        TokenStage.BASE,
                        null
                );

        String name = owner.getOwnerName();
        String newRefreshToken =
                jwtTokenProvider.createRefreshToken(owner);

        return TokenDto.fromEntity(newAccessToken, newRefreshToken, name);
    }

    public boolean verifyAndReleaseTable(String email, String password, Long customerTableId) {
        // 1. 현재 로그인된 Owner 정보 조회
        Owner owner = ownerRepository.findByOwnerEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        // 2. 비밀번호 먼저 검증
        boolean isMatched = passwordEncoder.matches(password, owner.getPassword());

        // 3. 비밀번호가 일치할 때만 테이블 상태 변경
        if (isMatched && customerTableId != null) {
            CustomerTable table = customerTableRepository.findById(customerTableId)
                    .orElseThrow(() -> new EntityNotFoundException("해당 테이블을 찾을 수 없습니다. (ID: " + customerTableId + ")"));

            // 테이블 상태를 STANDBY(이용 가능)로 변경
            table.updateStatus(TableStatus.STANDBY);
        }

        return isMatched;
    }

    public boolean verifySettlementKey(String email, String settlementKey) {
        Owner owner = ownerRepository.findByOwnerEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        log.info(">>> 정산키 검증 - email: {}, 입력값: [{}], DB값: [{}]",
                email, settlementKey, owner.getSettlementKey());
        boolean result = passwordEncoder.matches(settlementKey, owner.getSettlementKey());
        log.info(">>> 정산키 매칭 결과: {}", result);
        return result;
    }

    private String createCode(){
        return String.valueOf((int)(Math.random()*900000)+100000);
    }

    public void sendSignup(String phone) {
        String cooldownKey="SMS_COOLDOWN:"+phone;

        if(redis.hasKey(cooldownKey))
            throw new RuntimeException("1분 후 다시 요청");

        String code=createCode();

//        인증코드 3분 뒤 만료
        redis.opsForValue().set(
                "SMS_CODE:"+phone,
                code,
                CODE_TTL,
                TimeUnit.MINUTES
        );

        redis.opsForValue().set(
                cooldownKey,
                "wait",
                60,
                TimeUnit.SECONDS
        );

        smsSender.send(phone,code);
    }

    public void verifySignup(String phone,String input){

        String blockKey="SMS_BLOCK:"+phone;
        String failKey="SMS_FAIL:"+phone;

        if(redis.hasKey(blockKey))
            throw new RuntimeException("10분 차단");

        String saved=redis.opsForValue().get("SMS_CODE:"+phone);

        if(saved==null){
            fail(phone,failKey,blockKey);
            throw new RuntimeException("코드 만료");
        }

        if(!saved.equals(input)){
            fail(phone,failKey,blockKey);
            throw new RuntimeException("코드 불일치");
        }

        redis.delete(failKey);
        redis.delete("SMS_CODE:"+phone);
    }

    private void fail(String phone,String failKey,String blockKey){
        Long count=redis.opsForValue().increment(failKey);

        if(count==1)
            redis.expire(failKey,10,TimeUnit.MINUTES);

        if(count>=MAX_FAIL){
            redis.opsForValue().set(blockKey,"true",10,TimeUnit.MINUTES);
            redis.delete(failKey);
        }
    }
}
