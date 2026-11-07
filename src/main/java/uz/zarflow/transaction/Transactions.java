package uz.zarflow.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import uz.zarflow.category.Necessity;

/**
 * The transaction module's public API. Covers analytics' today/month summary and
 * everything the Telegram bot's quick-input flow needs (create/correct/undo a
 * bot-sourced entry) — Mini App CRUD/transfer/adjust is still driven straight by
 * {@link TransactionController} in this same package, with no need for a public seam.
 */
public interface Transactions {

    TodayAndMonth summarize(UUID userId, ZoneId zone);

    /** Idempotent on {@code input.idempotencyKey()} — a replayed Telegram update_id returns the same row. */
    BotTransaction createFromBot(UUID userId, BotCreateInput input);

    /** Used by the bot's "Kategoriya" button and the low-confidence category pick. */
    BotTransaction updateCategory(UUID userId, UUID transactionId, UUID categoryId);

    /** 404s (never 403) when the transaction doesn't exist or belongs to someone else. */
    void softDelete(UUID userId, UUID transactionId);

    /** Empty when it doesn't exist, belongs to someone else, or was already deleted. */
    Optional<BotTransaction> find(UUID userId, UUID transactionId);

    /** Today's income+expense entries (no transfers), newest first — for /today. */
    List<BotTransaction> listToday(UUID userId, ZoneId zone);

    /** Soft-deletes and returns the user's most recent non-deleted entry from the last 24h, if any — for /undo. */
    Optional<BotTransaction> undoLast(UUID userId);

    /** Income/expense/essential-discretionary totals and per-category expense breakdown for an arbitrary local date range. */
    PeriodAnalytics analyticsForRange(UUID userId, ZoneId zone, LocalDate from, LocalDate toInclusive);

    /** One entry per local day in the month that has at least one income/expense transaction — for the calendar heatmap. */
    List<DayTotal> dailyTotalsForMonth(UUID userId, ZoneId zone, YearMonth month);

    /** A single local day's income/expense transactions and totals — for the day drill-down screen. */
    DayDetail dayDetail(UUID userId, ZoneId zone, LocalDate date);

    /** A single local day's totals plus its single largest expense category — for the daily report. */
    DailyDigest dailyDigest(UUID userId, ZoneId zone, LocalDate date);

    record TodayAndMonth(
            List<ExpenseLine> todayExpenses,
            BigDecimal todayTotal,
            BigDecimal monthIncome,
            BigDecimal monthExpense,
            BigDecimal monthNet) {
    }

    record ExpenseLine(
            UUID id, UUID accountId, UUID categoryId, BigDecimal amount, String description, Instant transactionDate) {
    }

    record BotCreateInput(
            UUID accountId, UUID categoryId, TransactionType type, BigDecimal amount, Necessity necessity,
            String description, String idempotencyKey) {
    }

    record BotTransaction(
            UUID id, UUID accountId, UUID categoryId, TransactionType type, BigDecimal amount,
            String description, Instant transactionDate) {
    }

    record PeriodAnalytics(
            BigDecimal income,
            BigDecimal expense,
            BigDecimal essentialExpense,
            BigDecimal discretionaryExpense,
            List<CategoryAmount> expenseByCategory) {
    }

    record CategoryAmount(UUID categoryId, BigDecimal amount) {
    }

    record DayTotal(LocalDate date, BigDecimal income, BigDecimal expense, int count) {
    }

    record DayDetail(List<BotTransaction> transactions, BigDecimal income, BigDecimal expense) {
    }

    record DailyDigest(BigDecimal income, BigDecimal expense, int txCount, UUID topCategoryId, BigDecimal topCategoryAmount) {
    }

}
