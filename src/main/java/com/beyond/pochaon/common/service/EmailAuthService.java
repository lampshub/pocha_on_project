package com.beyond.pochaon.common.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class EmailAuthService {
    @Value("${spring.mail.username}")
    private String from;

    @Qualifier("emailVerify")
    private final RedisTemplate<String,String> redisTemplate;

    private final JavaMailSender mailSender;

    private static final long CODE_TTL = 5;
    private static final long VERIFIED_TTL = 10;

    public EmailAuthService(@Qualifier("emailVerify") RedisTemplate<String, String> redisTemplate, JavaMailSender mailSender) {
        this.redisTemplate = redisTemplate;
        this.mailSender = mailSender;
    }

    // 인증코드 발송
    public void sendCode(String email){

        String code = generateCode();

        redisTemplate.opsForValue().set(
                "EMAIL_CODE:"+email,
                code,
                CODE_TTL,
                TimeUnit.MINUTES
        );

        sendMail(email, code);
    }

    // 인증 검증
    public void verifyCode(String email,String code){

        String blockKey = "EMAIL_BLOCK:"+email;
        String failKey = "EMAIL_FAIL:"+email;

        //  차단 여부 확인
        if(Boolean.TRUE.equals(redisTemplate.hasKey(blockKey))){
            throw new RuntimeException("인증 5회 실패 → 10분 차단");
        }

        String saved = redisTemplate.opsForValue().get("EMAIL_CODE:"+email);

        //  인증 코드 없음
        if(saved == null){
            increaseFail(email, failKey, blockKey);
            throw new RuntimeException("코드 만료");
        }

        //  인증 코드 불일치
        if(!saved.equals(code)){
            increaseFail(email, failKey, blockKey);
            throw new RuntimeException("코드 불일치");
        }

        //  성공 → 실패카운트 제거
        redisTemplate.delete(failKey);

        redisTemplate.opsForValue().set(
                "EMAIL_VERIFIED:"+email,
                "true",
                VERIFIED_TTL,
                TimeUnit.MINUTES
        );
    }

    //    인증 코드 틀릴 경우 누적
    private void increaseFail(String email,String failKey,String blockKey){

        Long count = redisTemplate.opsForValue().increment(failKey);

        // 최초 실패면 TTL 설정
        if(count != null && count == 1){
            redisTemplate.expire(failKey,10,TimeUnit.MINUTES);
        }

        // 5회 이상 → block
        if(count != null && count >= 5){

            redisTemplate.opsForValue().set(
                    blockKey,
                    "true",
                    10,
                    TimeUnit.MINUTES
            );

            redisTemplate.delete(failKey);
        }
    }

    private String generateCode(){
        return String.valueOf((int)(Math.random()*900000)+100000);
    }

    private void sendMail(String email,String code){
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setFrom(from);
        msg.setSubject("이메일 인증코드");
        msg.setText("인증코드: "+code);
        mailSender.send(msg);
    }

    // 회원가입 시 인증 여부 확인
    public void checkVerified(String email){
        Boolean exists =
                redisTemplate.hasKey("EMAIL_VERIFIED:"+email);

        if(Boolean.FALSE.equals(exists))
            throw new RuntimeException("이메일 인증 필요");
    }

    // 가입 성공 후 삭제
    public void clear(String email){
        redisTemplate.delete("EMAIL_VERIFIED:"+email);
    }
}