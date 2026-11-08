package uz.zarflow.insight;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * The insight module's public API. Other modules talk to insights only through here —
 * never through {@code Insight}/{@code InsightRepository}.
 */
public interface Insights {

    /** Runs the rule engine for {@code today} and upserts any fired insight against its
     * (code, period_key) slot. Safe to call repeatedly in the same period -- a dismissed
     * insight is never resurrected, and a still-active one is just refreshed in place. */
    void evaluateAndStore(UUID userId, LocalDate today);

    List<ActiveInsight> listActive(UUID userId, Locale locale);

    /** 404s (never 403) when the insight doesn't exist or belongs to someone else. */
    void dismiss(UUID userId, UUID insightId);

    /** The single highest-priority insight never sent via bot, if any -- marks it sent so
     * it is never picked again. This is what caps bot delivery to one insight per sending. */
    Optional<RenderedInsight> takeTopUnsentForBot(UUID userId, Locale locale);

    record ActiveInsight(UUID id, InsightCode code, String icon, String text, boolean actionable, Instant createdAt) {
    }

    record RenderedInsight(UUID insightId, String text) {
    }

}
