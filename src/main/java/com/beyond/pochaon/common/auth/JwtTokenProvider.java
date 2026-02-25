package com.beyond.pochaon.common.auth;

import com.beyond.pochaon.owner.domain.Owner;
import com.beyond.pochaon.owner.repository.OwnerRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secretKey}")
    private String st_secret_key;

    @Value("${jwt.expirationAt}")
    private int expiration;

    private Key secret_key;

    @Value("${jwt.secretKeyRt}")
    private String st_secret_key_rt;

    @Value("${jwt.expirationRt}")
    private int expirationRt;

    private Key secret_key_rt;

    private final RedisTemplate<String, String> redisTemplate;
    private final OwnerRepository ownerRepository;

    @Autowired
    public JwtTokenProvider(
            @Qualifier("rtInventory")RedisTemplate<String, String> redisTemplate,
            OwnerRepository ownerRepository
    ) {
        this.redisTemplate = redisTemplate;
        this.ownerRepository = ownerRepository;
    }

    @PostConstruct
    public void init() {
        secret_key = new SecretKeySpec(
                Base64.getDecoder().decode(st_secret_key),
                SignatureAlgorithm.HS512.getJcaName()
        );

        secret_key_rt = new SecretKeySpec(
                Base64.getDecoder().decode(st_secret_key_rt),
                SignatureAlgorithm.HS512.getJcaName()
        );
    }

    /* =====================================================
       Access Token
       ===================================================== */

    // 점주 로그인 access token
    public String createBaseAccessToken(
            Owner owner,
            TokenStage stage,
            Map<String, Object> extraClaims
    ) {
        Claims claims = Jwts.claims()
                .setSubject(owner.getOwnerEmail());

        claims.put("role", owner.getRole().name());
        claims.put("stage", stage.name());
        claims.put("ownerId", owner.getId());

        if (extraClaims != null) {
            claims.putAll(extraClaims);
        }

        Date now = new Date();

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expiration * 60 * 1000L))
                .signWith(secret_key)
                .compact();
    }

    // access token 검증
    public Claims validateAccessToken(String accessToken) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secret_key)
                    .build()
                    .parseClaimsJws(accessToken)
                    .getBody();

        } catch (Exception e) {
            throw new IllegalArgumentException("유효하지 않은 AccessToken");
        }
    }

    // 매장 선택 후 토큰
    public String createStoreAccessToken(Owner owner, Long storeId) {

        Claims claims = Jwts.claims()
                .setSubject(owner.getOwnerEmail());

        claims.put("role", "OWNER");
        claims.put("stage", "STORE");
        claims.put("ownerId", owner.getId());
        claims.put("storeId", storeId);

        Date now = new Date();

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expiration * 60 * 1000L))
                .signWith(secret_key)
                .compact();
    }

    // 테이블 토큰
    public String createTableToken(String ownerEmail,Long storeId, int tableNum, Long customerTableId) {

        Claims claims = Jwts.claims()
                .setSubject("table-" + tableNum)
                .setSubject(ownerEmail);

        claims.put("stage", "TABLE");
        claims.put("role", "TABLE");
        claims.put("storeId", storeId);
        claims.put("tableNum", tableNum);
        claims.put("customerTableId", customerTableId);

        Date now = new Date();

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expiration * 60 * 1000L))
                .signWith(secret_key)
                .compact();
    }

    /* =====================================================
       Refresh Token
       ===================================================== */

    // refresh token 생성
    public String createRefreshToken(Owner owner) {

        Claims claims = Jwts.claims()
                .setSubject(owner.getOwnerEmail());

        claims.put("role", owner.getRole().name());

        Date now = new Date();

        String refreshToken = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationRt * 60 * 1000L))
                .signWith(secret_key_rt)
                .compact();

        // Redis 저장
        redisTemplate.opsForValue()
                .set(owner.getOwnerEmail(), refreshToken, expirationRt, TimeUnit.MINUTES);

        return refreshToken;
    }

    // refresh token 검증
    public Owner validateRefreshToken(String refreshToken) {
        String cleanedInputRt = refreshToken.replace("Bearer ", "").trim();

        Claims claims;

        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(secret_key_rt)
                    .build()
                    .parseClaimsJws(refreshToken)
                    .getBody();

        } catch (Exception e) {
            throw new IllegalArgumentException("유효하지 않은 RefreshToken");
        }

        String email = claims.getSubject();

        Owner owner = ownerRepository.findByOwnerEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Owner not found"));

        // Redis 토큰 검증
        String redisRt = redisTemplate.opsForValue().get(email);


        // 디버깅 로그 추가: 공백이나 "Bearer" 문자열이 포함되어 있는지 확인
        log.info("Cleaned Input RT: [" + cleanedInputRt + "]");
        log.info("Redis RT: [" + redisRt + "]");

        if (redisRt == null)
            throw new IllegalArgumentException("이미 사용된 RefreshToken");

        if (!redisRt.equals(cleanedInputRt))
            throw new IllegalArgumentException("RefreshToken mismatch");

        return owner;
    }

    public Integer getTableNum(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secret_key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("tableNum", Integer.class);
    }

    public Long getStoreId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secret_key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("storeId", Long.class);
    }

}

