package com.beyond.pochaon.owner.service;

import com.beyond.pochaon.common.auth.JwtTokenProvider;
import com.beyond.pochaon.common.auth.TokenStage;
import com.beyond.pochaon.owner.domain.Owner;
import com.beyond.pochaon.owner.dtos.BaseLoginDto;
import com.beyond.pochaon.owner.dtos.OwnerCreateDto;
import com.beyond.pochaon.owner.dtos.TokenDto;
import com.beyond.pochaon.owner.repository.OwnerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class OwnerLoginService {
    private final OwnerRepository ownerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate redisTemplate;

    public OwnerLoginService(OwnerRepository ownerRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider, @Qualifier("rtInventory")RedisTemplate redisTemplate) {
        this.ownerRepository = ownerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
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
        Owner owner = null;
        if (ownerRepository.findByOwnerEmail(dto.getOwnerEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 계정입니다.");
        } else {
            owner = ownerRepository.save(dto.toEntity(passwordEncoder.encode(dto.getPassword())));
        }
    }

    public TokenDto refresh(String refreshToken) {

        // 1. RT 검증 + Owner 조회
        Owner owner = jwtTokenProvider.validateRefreshToken(refreshToken);

        // 2. 기존 RT 폐기 (Rotation)
        redisTemplate.delete(owner.getOwnerEmail());

        // 3. 새로운 토큰 발급
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
}
