package uz.zarflow.humo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import uz.zarflow.transaction.TransactionType;

/**
 * The HUMO transaction-import module's public API (docs/PRD.md §1-5). The Telegram bot
 * detects a HUMO transaction message and drives the whole flow through here; this module
 * never touches Telegram types or formats user-facing text -- same split as
 * {@code Debts}/{@code Savings}/{@code HumoReconciliation}.
 *
 * <p>Category memory (user_parsing_memory) belongs to the telegram module, so it can't be
 * read/written from here directly (module boundary, docs/CLAUDE.md "modules talk via public
 * services or events, not repositories"). {@link #process} and {@link #pickDebt} (its
 * "ask category instead" branch) take a {@code categoryMemoryLookup} callback the bot layer
 * supplies -- checked before the module's own static merchant hints, per docs/PRD.md §4's
 * priority order. Remembering a category the user explicitly picked (vs. an auto-resolved
 * hint) is the bot layer's job too, exactly like the existing quick-input flow.
 */
public interface HumoImport {

    /** Parses {@code rawText} and processes every recognized transaction block: dedupe,
     * card-link gating, P2P/loan detection, category resolution (memory, then hints, then
     * {@link Outcome.NeedsCategory ask}), and the "possible manual duplicate" check.
     * {@code ProcessResult.parsed()} is false when the text has no recognizable HUMO
     * transaction block at all, so the caller can fall back to normal quick-input. */
    ProcessResult process(UUID userId, String rawText, Function<String, UUID> categoryMemoryLookup);

    /** [card picked] for a {@link Outcome.NeedsCardLink} draft -- links the card
     * (same {@code accounts.humo_card_last4} column balance reconciliation uses) and
     * continues straight into P2P/loan/category classification for this block. */
    Optional<Outcome> chooseAccount(UUID userId, UUID draftId, UUID accountId, Function<String, UUID> categoryMemoryLookup);

    /** [O'z hisobimga o'tkazma]/[Xarajat]/[Daromad] for a {@link Outcome.NeedsP2pChoice}
     * draft. Remembers the classification per merchant key; OWN_TRANSFER still asks which
     * destination account every time; EXPENSE/INCOME falls through to category resolution. */
    Optional<Outcome> chooseP2p(UUID userId, UUID draftId, P2pChoice choice, Function<String, UUID> categoryMemoryLookup);

    /** [destination account picked] for a {@link Outcome.NeedsP2pAccount} draft -- books
     * the TRANSFER between the two of the user's own accounts. */
    Optional<Outcome> chooseP2pAccount(UUID userId, UUID draftId, UUID destinationAccountId);

    /** [debt picked] for a {@link Outcome.NeedsLoanPick} draft -- {@code debtId} null means
     * "ask category instead" (no active debts, or the user declined every one offered). */
    Optional<Outcome> pickDebt(UUID userId, UUID draftId, UUID debtId, Function<String, UUID> categoryMemoryLookup);

    /** [category picked] for a {@link Outcome.NeedsCategory} draft -- runs the possible
     * manual-duplicate check before booking. */
    Optional<Outcome> pickCategory(UUID userId, UUID draftId, UUID categoryId);

    /** [Bu boshqa xarajat]/[Dublikat] for a {@link Outcome.NeedsDuplicateConfirm} draft. */
    Optional<Outcome> confirmDuplicate(UUID userId, UUID draftId, boolean isDuplicate);

    /** For the bot layer to build/validate the category keyboard and remember the user's
     * pick by merchant key -- empty once the draft is no longer awaiting a category (already
     * resolved, or belongs to someone else). */
    Optional<CategoryQuestion> findCategoryQuestion(UUID userId, UUID draftId);

    enum P2pChoice { OWN_TRANSFER, EXPENSE, INCOME }

    record CategoryQuestion(TransactionType type, String merchantKey) {
    }

    record ProcessResult(
            boolean parsed, int addedCount, int duplicateCount, int skippedNonUzsCount, List<Outcome> pending) {
    }

    /** One outcome per transaction block, either a final booking or a question still open. */
    sealed interface Outcome {

        record NeedsCardLink(UUID draftId, String cardLast4, TransactionType type, BigDecimal amount, String merchantRaw) implements Outcome {
        }

        record NeedsP2pChoice(UUID draftId, String merchantRaw, TransactionType type, BigDecimal amount) implements Outcome {
        }

        /** {@code ownAccountId} must be excluded from the destination-account picker --
         * the card's own linked account, not a valid transfer target for itself. */
        record NeedsP2pAccount(UUID draftId, UUID ownAccountId, BigDecimal amount) implements Outcome {
        }

        record NeedsLoanPick(UUID draftId, String merchantRaw, BigDecimal amount) implements Outcome {
        }

        record NeedsCategory(UUID draftId, TransactionType type, BigDecimal amount, String merchantRaw) implements Outcome {
        }

        record NeedsDuplicateConfirm(UUID draftId, TransactionType type, BigDecimal amount, String merchantRaw) implements Outcome {
        }

        record Booked(TransactionType type, BigDecimal amount, UUID transactionId) implements Outcome {
        }

        /** The user confirmed [Dublikat] on a {@link NeedsDuplicateConfirm} draft -- not
         * booked, counted as a duplicate instead. */
        record Skipped(TransactionType type, BigDecimal amount) implements Outcome {
        }
    }

}
