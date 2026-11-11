package uz.zarflow.account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountBalanceMathTest {

    private static final Instant JAN_1 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant JAN_10 = Instant.parse("2026-01-10T00:00:00Z");
    private static final Instant JAN_20 = Instant.parse("2026-01-20T00:00:00Z");

    @Test
    void asOfIncludesOnlyEntriesUpToAndIncludingTheCutoff() {
        List<AccountBalanceMath.Entry> ledger = List.of(
                new AccountBalanceMath.Entry("INCOME", new BigDecimal("500000.00"), JAN_1),
                new AccountBalanceMath.Entry("EXPENSE", new BigDecimal("100000.00"), JAN_10),
                new AccountBalanceMath.Entry("EXPENSE", new BigDecimal("50000.00"), JAN_20));

        BigDecimal balanceAtCutoff = AccountBalanceMath.asOf(BigDecimal.ZERO, ledger, JAN_10);

        assertThat(balanceAtCutoff).isEqualByComparingTo("400000.00");
    }

    @Test
    void entryExactlyAtTheCutoffIsIncluded() {
        List<AccountBalanceMath.Entry> ledger = List.of(
                new AccountBalanceMath.Entry("INCOME", new BigDecimal("1000.00"), JAN_10));

        BigDecimal balance = AccountBalanceMath.asOf(BigDecimal.ZERO, ledger, JAN_10);

        assertThat(balance).isEqualByComparingTo("1000.00");
    }

    @Test
    void debtPaymentSubtractsLikeExpense() {
        List<AccountBalanceMath.Entry> ledger = List.of(
                new AccountBalanceMath.Entry("DEBT_PAYMENT", new BigDecimal("30000.00"), JAN_1));

        BigDecimal balance = AccountBalanceMath.asOf(new BigDecimal("100000.00"), ledger, JAN_20);

        assertThat(balance).isEqualByComparingTo("70000.00");
    }

    @Test
    void transferAddsAsStoredRegardlessOfSign() {
        List<AccountBalanceMath.Entry> ledger = List.of(
                new AccountBalanceMath.Entry("TRANSFER", new BigDecimal("-20000.00"), JAN_1),
                new AccountBalanceMath.Entry("TRANSFER", new BigDecimal("20000.00"), JAN_10));

        BigDecimal balance = AccountBalanceMath.asOf(new BigDecimal("50000.00"), ledger, JAN_20);

        assertThat(balance).isEqualByComparingTo("50000.00");
    }

}