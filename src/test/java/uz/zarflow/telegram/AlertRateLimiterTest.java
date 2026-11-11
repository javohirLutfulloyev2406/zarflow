package uz.zarflow.telegram;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlertRateLimiterTest {

    private static final Duration COOLDOWN = Duration.ofMinutes(15);

    private Instant now = Instant.parse("2026-11-12T10:00:00Z");

    private final Clock clock = new Clock() {
        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return now;
        }
    };

    private final AlertRateLimiter limiter = new AlertRateLimiter(clock, COOLDOWN, 20);

    @Test
    void firstOccurrenceIsAlwaysAllowedWithNoSuppressedCount() {
        var decision = limiter.tryAcquire("sig-a");
        assertThat(decision.allowed()).isTrue();
        assertThat(decision.suppressedCount()).isZero();
    }

    @Test
    void repeatWithinCooldownIsSuppressed() {
        limiter.tryAcquire("sig-a");
        var decision = limiter.tryAcquire("sig-a");
        assertThat(decision.allowed()).isFalse();
        assertThat(decision.suppressedCount()).isEqualTo(1);
    }

    @Test
    void multipleRepeatsWithinCooldownAccumulateSuppressedCount() {
        limiter.tryAcquire("sig-a");
        limiter.tryAcquire("sig-a");
        limiter.tryAcquire("sig-a");
        var decision = limiter.tryAcquire("sig-a");
        assertThat(decision.allowed()).isFalse();
        assertThat(decision.suppressedCount()).isEqualTo(3);
    }

    @Test
    void afterCooldownElapsesNextOccurrenceIsAllowedAndReportsSuppressedCount() {
        limiter.tryAcquire("sig-a");
        limiter.tryAcquire("sig-a");
        limiter.tryAcquire("sig-a");

        now = now.plus(COOLDOWN).plusSeconds(1);

        var decision = limiter.tryAcquire("sig-a");
        assertThat(decision.allowed()).isTrue();
        assertThat(decision.suppressedCount()).isEqualTo(2);
    }

    @Test
    void suppressedCountResetsAfterASuccessfulSend() {
        limiter.tryAcquire("sig-a");
        limiter.tryAcquire("sig-a");
        now = now.plus(COOLDOWN).plusSeconds(1);
        limiter.tryAcquire("sig-a");

        var decision = limiter.tryAcquire("sig-a");
        assertThat(decision.allowed()).isFalse();
        assertThat(decision.suppressedCount()).isEqualTo(1);
    }

    @Test
    void differentSignaturesAreTrackedIndependently() {
        limiter.tryAcquire("sig-a");
        var decisionB = limiter.tryAcquire("sig-b");
        assertThat(decisionB.allowed()).isTrue();
        assertThat(decisionB.suppressedCount()).isZero();
    }

    @Test
    void hourlyCapBlocksFurtherSendsEvenForNewSignatures() {
        AlertRateLimiter tightLimiter = new AlertRateLimiter(clock, Duration.ZERO, 2);
        assertThat(tightLimiter.tryAcquire("sig-1").allowed()).isTrue();
        assertThat(tightLimiter.tryAcquire("sig-2").allowed()).isTrue();

        var decision = tightLimiter.tryAcquire("sig-3");
        assertThat(decision.allowed()).isFalse();
    }

    @Test
    void hourlyCapRecoversAfterAnHourPasses() {
        AlertRateLimiter tightLimiter = new AlertRateLimiter(clock, Duration.ZERO, 1);
        assertThat(tightLimiter.tryAcquire("sig-1").allowed()).isTrue();
        assertThat(tightLimiter.tryAcquire("sig-2").allowed()).isFalse();

        now = now.plus(Duration.ofHours(1)).plusSeconds(1);

        assertThat(tightLimiter.tryAcquire("sig-2").allowed()).isTrue();
    }

}
