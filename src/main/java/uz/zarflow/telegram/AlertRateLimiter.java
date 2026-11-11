package uz.zarflow.telegram;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decides whether an admin alert for a given signature may actually be sent right now.
 * Pure logic, no Spring -- {@link AdminAlerterService} owns the Telegram call and the
 * signature string; this class only tracks timing. Two independent limits apply:
 * per-signature cooldown (dedupe repeats of the same error) and a global hourly cap
 * (stop a storm of distinct errors from flooding the admin chat).
 */
final class AlertRateLimiter {

    private final Clock clock;
    private final Duration cooldown;
    private final int maxPerHour;
    private final Map<String, SignatureState> bySignature = new ConcurrentHashMap<>();
    private final Deque<Instant> hourlySends = new ArrayDeque<>();

    AlertRateLimiter(Clock clock, Duration cooldown, int maxPerHour) {
        this.clock = clock;
        this.cooldown = cooldown;
        this.maxPerHour = maxPerHour;
    }

    /** Thread-safe: the hourly window is shared mutable state across all signatures. */
    synchronized Decision tryAcquire(String signature) {
        Instant now = Instant.now(clock);
        pruneHourlyWindow(now);

        SignatureState state = bySignature.computeIfAbsent(signature, key -> new SignatureState());
        boolean cooldownElapsed = state.lastSentAt == null || now.isAfter(state.lastSentAt.plus(cooldown));
        boolean underHourlyCap = hourlySends.size() < maxPerHour;

        if (cooldownElapsed && underHourlyCap) {
            int suppressedSinceLastSend = state.suppressedCount;
            state.suppressedCount = 0;
            state.lastSentAt = now;
            hourlySends.addLast(now);
            return new Decision(true, suppressedSinceLastSend);
        }

        state.suppressedCount++;
        return new Decision(false, state.suppressedCount);
    }

    private void pruneHourlyWindow(Instant now) {
        Instant cutoff = now.minus(Duration.ofHours(1));
        while (!hourlySends.isEmpty() && hourlySends.peekFirst().isBefore(cutoff)) {
            hourlySends.pollFirst();
        }
    }

    /** {@code suppressedCount}: on {@code allowed=true}, how many occurrences of this
     * signature were dropped since the last time it was actually sent (0 the first time);
     * on {@code allowed=false}, the running total so far this cooldown/cap window. */
    record Decision(boolean allowed, int suppressedCount) {
    }

    private static final class SignatureState {
        Instant lastSentAt;
        int suppressedCount;
    }

}
