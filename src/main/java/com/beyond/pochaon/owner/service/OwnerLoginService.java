package com.beyond.pochaon.owner.service;

import com.beyond.pochaon.common.auth.JwtTokenProvider;
import com.beyond.pochaon.common.auth.TokenStage;
import com.beyond.pochaon.customerTable.domain.CustomerTable;
import com.beyond.pochaon.customerTable.domain.TableStatus;
import com.beyond.pochaon.customerTable.repository.CustomerTableRepository;
import com.beyond.pochaon.owner.domain.Owner;
import com.beyond.pochaon.owner.dtos.BaseLoginDto;
import com.beyond.pochaon.owner.dtos.OwnerCreateDto;
import com.beyond.pochaon.owner.dtos.TokenDto;
import com.beyond.pochaon.owner.repository.OwnerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OwnerLoginService {
    private final OwnerRepository ownerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomerTableRepository customerTableRepository;

    public OwnerLoginService(OwnerRepository ownerRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider, CustomerTableRepository customerTableRepository) {
        this.ownerRepository = ownerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.customerTableRepository = customerTableRepository;
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
            ownerRepository.save(dto.toEntity(passwordEncoder.encode(dto.getPassword())));
        }
    }

    public TokenDto refresh(String refreshToken) {

        // 1. RT 검증 + Owner 조회
        Owner owner = jwtTokenProvider.validateRefreshToken(refreshToken);


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
}
