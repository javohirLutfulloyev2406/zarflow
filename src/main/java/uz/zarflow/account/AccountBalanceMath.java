package uz.zarflow.account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * An account's balance as of an arbitrary past instant -- pure arithmetic over a ledger
 * already fetched from {@link AccountRepository#findLedgerRowsByAccountId}, so Humo
 * reconciliation's "as of the balance message's timestamp" filtering (docs/PRD.md §7) is
 * unit-testable without Spring/Postgres, the same way {@code BudgetCalculator} is.
 */
final class AccountBalanceMath {

    private AccountBalanceMath() {
    }

    /** Mirrors the sign convention in {@code AccountRepository.findBalancesByUserId}: EXPENSE
     * and DEBT_PAYMENT subtract, everything else (INCOME, TRANSFER) adds as stored. */
    static BigDecimal asOf(BigDecimal initialBalance, List<Entry> ledger, Instant asOf) {
        BigDecimal total = initialBalance;
        for (Entry entry : ledger) {
            if (entry.transactionDate().isAfter(asOf)) {
                continue;
            }
            boolean subtracts = "EXPENSE".equals(entry.type()) || "DEBT_PAYMENT".equals(entry.type());
            total = subtracts ? total.subtract(entry.amount()) : total.add(entry.amount());
        }
        return total;
    }

    record Entry(String type, BigDecimal amount, Instant transactionDate) {
    }

}