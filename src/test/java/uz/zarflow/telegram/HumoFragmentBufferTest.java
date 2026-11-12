package uz.zarflow.telegram;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HumoFragmentBufferTest {

    private Instant now = Instant.parse("2026-11-12T07:00:00Z");

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

    private final UUID userId = UUID.randomUUID();
    private final Locale locale = Locale.of("uz");

    private final HumoFragmentBuffer buffer = new HumoFragmentBuffer(clock);

    @Test
    void freshlyOfferedLineIsNotYetReady() {
        buffer.offer(1L, userId, locale, "💸 Оплата", null);

        assertThat(buffer.drainReady()).isEmpty();
    }

    @Test
    void becomesReadyOnceDebounceElapsesSinceTheLastLine() {
        buffer.offer(1L, userId, locale, "💸 Оплата", null);
        now = now.plus(HumoFragmentBuffer.DEBOUNCE).plusMillis(1);

        List<HumoFragmentBuffer.ReadyBatch> ready = buffer.drainReady();

        assertThat(ready).hasSize(1);
        assertThat(ready.get(0).lines()).containsExactly("💸 Оплата");
    }

    @Test
    void aNewLineResetsTheDebounceForThatChat() {
        buffer.offer(1L, userId, locale, "💸 Оплата", null);
        now = now.plus(HumoFragmentBuffer.DEBOUNCE).minusMillis(1);
        buffer.offer(1L, userId, locale, "➖ 47.000,00 UZS", null);

        assertThat(buffer.drainReady()).isEmpty();

        now = now.plus(HumoFragmentBuffer.DEBOUNCE).plusMillis(1);
        List<HumoFragmentBuffer.ReadyBatch> ready = buffer.drainReady();

        assertThat(ready).hasSize(1);
        assertThat(ready.get(0).lines()).containsExactly("💸 Оплата", "➖ 47.000,00 UZS");
    }

    @Test
    void differentChatsAreBufferedIndependently() {
        buffer.offer(1L, userId, locale, "💸 Оплата", null);
        now = now.plus(HumoFragmentBuffer.DEBOUNCE).plusMillis(1);
        buffer.offer(2L, userId, locale, "📍 OTHER MERCHANT", null);

        List<HumoFragmentBuffer.ReadyBatch> ready = buffer.drainReady();

        assertThat(ready).hasSize(1);
        assertThat(ready.get(0).chatId()).isEqualTo(1L);
    }

    @Test
    void firstFragmentsForwardDateIsKeptEvenIfLaterOnesHaveNone() {
        Instant forwardDate = Instant.parse("2026-11-12T06:59:00Z");
        buffer.offer(1L, userId, locale, "💸 Оплата", forwardDate);
        buffer.offer(1L, userId, locale, "➖ 47.000,00 UZS", null);
        now = now.plus(HumoFragmentBuffer.DEBOUNCE).plusMillis(1);

        List<HumoFragmentBuffer.ReadyBatch> ready = buffer.drainReady();

        assertThat(ready.get(0).forwardDate()).isEqualTo(forwardDate);
    }

    @Test
    void drainedBucketIsRemovedAndWontReappear() {
        buffer.offer(1L, userId, locale, "💸 Оплата", null);
        now = now.plus(HumoFragmentBuffer.DEBOUNCE).plusMillis(1);
        buffer.drainReady();

        assertThat(buffer.drainReady()).isEmpty();
    }

}
