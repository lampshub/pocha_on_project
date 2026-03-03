package com.beyond.pochaon.store.dto;

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

    // ── 날짜 범위 직접 지정 (우선 적용) ──
    private String startDate;  // "yyyy-MM-dd"
    private String endDate;    // "yyyy-MM-dd"

    private LocalDate getSafeLocalDate() {
        YearMonth ym = YearMonth.of(year, month);
        int targetDay = Math.max(1, Math.min(day, ym.lengthOfMonth()));
        return LocalDate.of(year, month, targetDay);
    }

    /** startDate/endDate가 있으면 우선 사용 */
    private boolean hasExplicitRange() {
        return startDate != null && !startDate.isEmpty()
                && endDate != null && !endDate.isEmpty();
    }

    public LocalDateTime getStartAt() {
        if (hasExplicitRange()) {
            return LocalDate.parse(startDate).atStartOfDay();
        }
        if ("weekly".equals(viewMode)) {
            return getWeekStart().atStartOfDay();
        }
        if ("monthly".equals(viewMode) || day == 0) {
            return YearMonth.of(year, month).atDay(1).atStartOfDay();
        }
        return LocalDate.of(year, month, day).atStartOfDay();
    }

    public LocalDateTime getEndAt() {
        if (hasExplicitRange()) {
            return LocalDate.parse(endDate).plusDays(1).atStartOfDay();
        }
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