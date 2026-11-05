package uz.zarflow.shared.common;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateRangeTest {

    @Test
    void thisWeekStartsMonday() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-04T10:00:00Z"), ZoneOffset.UTC);

        DateRange week = DateRange.thisWeek(clock, ZoneOffset.UTC);

        assertThat(week.from()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(week.toInclusive()).isEqualTo(LocalDate.of(2026, 11, 8));
    }

    @Test
    void ofMonthHandlesLeapFebruary() {
        DateRange range = DateRange.ofMonth(YearMonth.of(2028, 2));

        assertThat(range.days()).isEqualTo(29);
        assertThat(range.toInclusive()).isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    void dayBoundaryUsesUserTimezoneNotUtc() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-30T19:30:00Z"), ZoneOffset.UTC);
        ZoneId tashkent = ZoneId.of("Asia/Tashkent");

        DateRange today = DateRange.today(clock, tashkent);

        assertThat(today.from()).isEqualTo(LocalDate.of(2026, 12, 1));
    }

    @Test
    void toInstantRangeEndIsExclusive() {
        DateRange singleDay = DateRange.custom(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 1));

        DateRange.InstantRange instantRange = singleDay.toInstantRange(ZoneOffset.UTC);

        assertThat(instantRange.endExclusive()).isEqualTo(instantRange.startInclusive().plusSeconds(24 * 3600));
    }

    @Test
    void remainingDaysIncludingOnLastDayIsOne() {
        DateRange range = DateRange.custom(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        assertThat(range.remainingDaysIncluding(LocalDate.of(2026, 11, 30))).isEqualTo(1);
    }

    @Test
    void remainingDaysIncludingRejectsDateOutsideRange() {
        DateRange range = DateRange.custom(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        assertThatThrownBy(() -> range.remainingDaysIncluding(LocalDate.of(2026, 12, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsFromAfterTo() {
        assertThatThrownBy(() -> DateRange.custom(LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
