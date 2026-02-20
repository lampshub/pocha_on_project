package com.beyond.pochaon.owner.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class TokenDto {
    private String accessToken;
    private String refreshToken;
    private String name;

    public static TokenDto fromEntity(String accessToken, String refreshToken, String name) {
        return TokenDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .name(name)
                .build();
    }
}
