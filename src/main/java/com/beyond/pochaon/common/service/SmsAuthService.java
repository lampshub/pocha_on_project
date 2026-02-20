package com.beyond.pochaon.common.service;

import com.beyond.pochaon.owner.domain.Owner;
import com.beyond.pochaon.owner.repository.OwnerRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class SmsAuthService {
    @Qualifier("smsVerify")
    private final RedisTemplate<String,String> redis;
    private final SmsSender smsSender;
    private final OwnerRepository ownerRepository;

    private static final long CODE_TTL = 3;
    private static final int MAX_FAIL = 5;

    public SmsAuthService(@Qualifier("smsVerify") RedisTemplate<String, String> redis, SmsSender smsSender, OwnerRepository ownerRepository) {
        this.redis = redis;
        this.smsSender = smsSender;
        this.ownerRepository = ownerRepository;
    }

    // 인증번호 발송
    public void send(String name,String phone){

        ownerRepository.findByOwnerNameAndPhoneNumber(name,phone)
                .orElseThrow(() -> new RuntimeException("회원 없음"));

        String cooldownKey="SMS_COOLDOWN:"+phone;

        if(Boolean.TRUE.equals(redis.hasKey(cooldownKey)))
            throw new RuntimeException("1분 후 다시 요청");

        String code=create();

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


    // 인증번호 검증 + 이메일 반환
    public String verify(String name,String phone,String input){

        String blockKey="SMS_BLOCK:"+phone;
        String failKey="SMS_FAIL:"+phone;

        if(Boolean.TRUE.equals(redis.hasKey(blockKey)))
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

        Owner owner=ownerRepository
                .findByOwnerNameAndPhoneNumber(name,phone)
                .orElseThrow(() -> new RuntimeException("회원 없음"));

        redis.delete("SMS_CODE:"+phone);

        return mask(owner.getOwnerEmail());
    }


//    5회 실패 시 10분간 접근 불가
    private void fail(String phone,String failKey,String blockKey){
        Long count=redis.opsForValue().increment(failKey);

        if(count==1)
            redis.expire(failKey,10,TimeUnit.MINUTES);

        if(count>=MAX_FAIL){
            redis.opsForValue().set(blockKey,"true",10,TimeUnit.MINUTES);
            redis.delete(failKey);
        }
    }

//    6자리 난수 인증 코드 생성
    private String create(){
        return String.valueOf((int)(Math.random()*900000)+100000);
    }

//    로그인 아이디(점주 이메일) 마스킹
    private String mask(String email){
        String[] p=email.split("@");
        String id=p[0];

        if(id.length()<=3)
            return id.charAt(0)+"**@"+p[1];

        return id.substring(0,2)+"***@"+p[1];
    }
}

