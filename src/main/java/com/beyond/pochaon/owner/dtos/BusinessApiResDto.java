package com.beyond.pochaon.owner.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class BusinessApiResDto {

    private String status_code;     //API 상태코드 => OK, error
//    private Integer match_cnt;   // 매칭된 수
//    private Integer request_cnt; // 요청 수
    private String message;         //실패시 응답메세지
    private List<BusinessData> data;

    @Data
    @NoArgsConstructor
    public static class  BusinessData {
        private String b_no;           // 사업자등록번호
        private String b_stt;          // 납세자상태 (계속사업자, 휴업자 등)
        private String b_stt_cd;       // 납세자상태코드
        private String tax_type;       // 과세유형
        private String tax_type_cd;    // 과세유형코드
        private String end_dt;         // 폐업일
        private String utcc_yn;        // 단위과세전환폐업여부
        private String tax_type_change_dt; // 과세유형전환일자
        private String invoice_apply_dt;   // 전자세금계산서적용일자
        private String valid;          // 검증결과 ("01": 확인, "02": 불일치)
    }
}
