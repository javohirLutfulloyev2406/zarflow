package uz.zarflow.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * The transaction module's public API. Only exposes what another module
 * (analytics' today/month summary) needs — everything else (CRUD, transfer,
 * adjust) is driven straight by {@link TransactionController} in this same
 * package, with no need for a public seam.
 */
public interface Transactions {

    TodayAndMonth summarize(UUID userId, ZoneId zone);

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

}
