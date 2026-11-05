package uz.zarflow.shared.common;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * An inclusive date range. {@code today}/{@code thisWeek}/{@code thisMonth} take an
 * explicit {@link Clock} and {@link ZoneId} because "what day is it" depends on the
 * user's timezone (PRD rule 6) — the app's {@code Clock} bean stays UTC, callers
 * localize it here.
 */
public record DateRange(LocalDate from, LocalDate toInclusive) {

    public DateRange {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(toInclusive, "toInclusive");
        if (from.isAfter(toInclusive)) {
            throw new IllegalArgumentException("from (" + from + ") is after toInclusive (" + toInclusive + ")");
        }
    }

    public static DateRange today(Clock clock, ZoneId zone) {
        LocalDate today = LocalDate.now(clock.withZone(zone));
        return new DateRange(today, today);
    }

    public static DateRange thisWeek(Clock clock, ZoneId zone) {
        LocalDate today = LocalDate.now(clock.withZone(zone));
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        return new DateRange(monday, monday.plusDays(6));
    }

    public static DateRange thisMonth(Clock clock, ZoneId zone) {
        YearMonth month = YearMonth.from(LocalDate.now(clock.withZone(zone)));
        return ofMonth(month);
    }

    public static DateRange ofMonth(YearMonth month) {
        return new DateRange(month.atDay(1), month.atEndOfMonth());
    }

    public static DateRange custom(LocalDate from, LocalDate to) {
        return new DateRange(from, to);
    }

    public long days() {
        return ChronoUnit.DAYS.between(from, toInclusive) + 1;
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(from) && !date.isAfter(toInclusive);
    }

    public long remainingDaysIncluding(LocalDate today) {
        if (!contains(today)) {
            throw new IllegalArgumentException(today + " is outside range " + this);
        }
        return ChronoUnit.DAYS.between(today, toInclusive) + 1;
    }

    public InstantRange toInstantRange(ZoneId zone) {
        Instant start = from.atStartOfDay(zone).toInstant();
        Instant end = toInclusive.plusDays(1).atStartOfDay(zone).toInstant();
        return new InstantRange(start, end);
    }

    /** [startInclusive, endExclusive) — ready to use as a half-open query bound. */
    public record InstantRange(Instant startInclusive, Instant endExclusive) {
    }

}
