package com.beyond.pochaon.owner.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class BusinessApiReqDto {

    @Valid
    private List<BusinessRequest> businesses;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BusinessRequest {

        @NotBlank(message = "사업자등록번호는 필수입니다")
        @Pattern(regexp = "^\\d{10}$", message = "사업자등록번호는 10자리 숫자여야 합니다")
        private String b_no;        // 사업자등록번호 : -없이 10자리

        @NotBlank(message = "대표자명은 필수입니다")
        private String p_nm;        // 대표자명

        @NotBlank(message = "개업일자는 필수입니다")
        @Pattern(regexp = "^\\d{8}$", message = "개업일자는 8자리 숫자(YYYYMMDD)여야 합니다")
        private String start_dt;    // 개업일자 : yyyyMMdd -> 형식 지켜야함
    }
}
