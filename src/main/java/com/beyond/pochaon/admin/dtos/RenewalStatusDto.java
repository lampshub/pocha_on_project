package com.beyond.pochaon.admin.dtos;

import com.beyond.pochaon.store.domain.RenewalRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RenewalStatusDto {
    private RenewalRequestStatus status;
    private String rejectedReason;
    private LocalDateTime processedAt;
}
