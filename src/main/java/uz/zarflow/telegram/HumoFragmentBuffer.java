package uz.zarflow.telegram;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Holds not-yet-complete HUMO notification lines per chat for a short debounce window.
 * @HUMOcardbot forwards routinely arrive as several consecutive Telegram messages (one
 * per line) rather than one combined message, so a lone line like "🕓 12:00 12.11.2026"
 * never parses as a full transaction block on its own ({@code HumoFragmentDetector}
 * flags it as a fragment instead of handing it to quick-input). {@link
 * HumoFragmentScheduler} re-merges and reparses each chat's buffered lines once no new
 * fragment has arrived for {@link #DEBOUNCE}.
 *
 * <p>In-memory only, single-instance: a restart mid-debounce silently drops whatever was
 * buffered (see ADR 0005). Acceptable here given the ~1.2s window and this bot's traffic.
 */
@Component
class HumoFragmentBuffer {

    static final Duration DEBOUNCE = Duration.ofMillis(1200);

    private final Clock clock;
    private final Map<Long, Bucket> buckets = new ConcurrentHashMap<>();

    HumoFragmentBuffer(Clock clock) {
        this.clock = clock;
    }

    /** {@code forwardDate} is the triggering message's own forward timestamp, if any --
     * only the first fragment's matters, matching single-message {@code resolveForwardDate}. */
    void offer(long chatId, UUID userId, Locale locale, String line, Instant forwardDate) {
        buckets.compute(chatId, (id, existing) -> {
            Bucket bucket = existing != null ? existing : new Bucket(userId, locale, forwardDate);
            bucket.lines.add(line);
            bucket.lastReceivedAt = Instant.now(clock);
            return bucket;
        });
    }

    /** Removes and returns every bucket that's been quiet for at least {@link #DEBOUNCE}. */
    List<ReadyBatch> drainReady() {
        Instant now = Instant.now(clock);
        List<ReadyBatch> ready = new ArrayList<>();
        buckets.entrySet().removeIf(entry -> {
            Bucket bucket = entry.getValue();
            if (Duration.between(bucket.lastReceivedAt, now).compareTo(DEBOUNCE) < 0) {
                return false;
            }
            ready.add(new ReadyBatch(
                    entry.getKey(), bucket.userId, bucket.locale, List.copyOf(bucket.lines), bucket.forwardDate));
            return true;
        });
        return ready;
    }

    private static final class Bucket {
        final UUID userId;
        final Locale locale;
        final Instant forwardDate;
        final List<String> lines = new ArrayList<>();
        volatile Instant lastReceivedAt;

        Bucket(UUID userId, Locale locale, Instant forwardDate) {
            this.userId = userId;
            this.locale = locale;
            this.forwardDate = forwardDate;
        }
    }

    record ReadyBatch(long chatId, UUID userId, Locale locale, List<String> lines, Instant forwardDate) {
    }

}
