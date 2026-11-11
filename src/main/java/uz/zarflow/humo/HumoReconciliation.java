package uz.zarflow.humo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import uz.zarflow.transaction.TransactionType;

/**
 * The Humo balance-reconciliation module's public API (docs/PRD.md §7). The Telegram bot
 * detects a balance message and drives the whole flow through here; this module never
 * touches Telegram types or formats user-facing text -- that's the bot layer's job, same
 * split as {@code Debts}/{@code Savings}/{@code Recurring}.
 */
public interface HumoReconciliation {

    /** Parses {@code rawText} and reconciles every recognized card block. {@code wasForwarded}
     * is false for pasted text -- the caller should disclose that "now" was used as the as-of
     * time in that case. {@code ProcessResult.parsed()} is false when the text has no
     * recognizable Humo card block at all, so the caller can fall back to normal quick-input. */
    ProcessResult process(UUID userId, String rawText, Instant asOf, boolean wasForwarded);

    /** [Tuzatish] -- books the correction transaction. Empty when the draft is gone (already
     * acted on, or belongs to someone else). */
    Optional<CorrectResult> correct(UUID userId, UUID draftId);

    /** [O'tkazib yuborish] -- dismiss a pending mismatch draft without acting on it. */
    void skip(UUID userId, UUID draftId);

    /** [Hisobga bog'lash] -&gt; account chosen -- links the card to this account and, if a real
     * diff remains, keeps the draft alive for an immediate Tuzatish/O'tkazib yuborish follow-up. */
    Optional<LinkResult> link(UUID userId, UUID draftId, UUID accountId);

    /** [E'tiborsiz qoldirish] -- remember to stop offering to link this card. */
    void ignore(UUID userId, UUID draftId);

    enum Kind { MATCHED, DIFF, UNLINKED }

    /** {@code zarflowBalance}/{@code diff} are null for {@link Kind#UNLINKED}; {@code draftId}
     * is null for {@link Kind#MATCHED} (nothing to act on). {@code showHint} is true only the
     * first time ever a correction is offered to this user (docs/PRD.md §7 one-time hint). */
    record CardResult(
            String cardLast4, Kind kind, UUID draftId,
            BigDecimal bankBalance, BigDecimal zarflowBalance, BigDecimal diff, boolean showHint) {
    }

    /** {@code parsed} false means "not a Humo balance message at all" (no card blocks found);
     * {@code duplicate} true means the exact same message was already processed. */
    record ProcessResult(boolean parsed, boolean duplicate, List<CardResult> cards, int emptyOrIgnoredCount) {
    }

    /** {@code type}/{@code amount} are null/zero when the balance already matched by the time
     * the user tapped Tuzatish (someone else's correction, or a fresher sync, beat them to it). */
    record CorrectResult(TransactionType type, BigDecimal amount) {
    }

    /** {@code matched} true means linking already closed the gap -- nothing left to correct. */
    record LinkResult(boolean matched, UUID draftId, BigDecimal diff, boolean showHint) {
    }

}