package com.beyond.pochaon.store.settlementdto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodReqDto {

    private int year;
    private int month;
    @Builder.Default
    private int day = 0;

    private String period;
    private String viewMode; // "daily", "weekly", "monthly"

    // 내부적으로 안전한 날짜를 가져오기 위한 헬퍼 메서드
    private LocalDate getSafeLocalDate() {
        YearMonth ym = YearMonth.of(year, month);
        // day가 0 이하이면 1일로, 월 최대 일수보다 크면 마지막 날로 보정
        int targetDay = Math.max(1, Math.min(day, ym.lengthOfMonth()));
        return LocalDate.of(year, month, targetDay);
    }

    // ── 일별 범위 ──
    public LocalDateTime getStartAt() {
        if ("weekly".equals(viewMode)) {
            return getWeekStart().atStartOfDay();
        }
        if ("monthly".equals(viewMode) || day == 0) {
            return YearMonth.of(year, month).atDay(1).atStartOfDay();
        }
        return LocalDate.of(year, month, day).atStartOfDay();
    }

    public LocalDateTime getEndAt() {
        if ("weekly".equals(viewMode)) {
            return getWeekEnd().plusDays(1).atStartOfDay();
        }
        if ("monthly".equals(viewMode) || day == 0) {
            return YearMonth.of(year, month).plusMonths(1).atDay(1).atStartOfDay();
        }
        return LocalDate.of(year, month, day).plusDays(1).atStartOfDay();
    }

    // ── 주간 범위 (월~일 기준) ──

    public LocalDate getWeekStart() {
        // day가 0이어도 getSafeLocalDate가 1일을 반환하므로 안전함
        return getSafeLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public LocalDate getWeekEnd() {
        return getSafeLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }

    // ── 월별 범위 ──
    public LocalDate getMonthStart() {
        return LocalDate.of(year, month, 1);
    }

    public LocalDate getMonthEnd() {
        // 다음 달 1일
        return YearMonth.of(year, month).plusMonths(1).atDay(1);
    }
}