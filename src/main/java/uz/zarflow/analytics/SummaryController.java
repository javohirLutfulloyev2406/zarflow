package uz.zarflow.analytics;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uz.zarflow.account.Accounts;
import uz.zarflow.debt.Debts;
import uz.zarflow.shared.security.CurrentUser;
import uz.zarflow.transaction.Transactions;
import uz.zarflow.user.UserProfiles;

@RestController
@RequestMapping("/api/v1/summary")
class SummaryController {

    private final Transactions transactions;
    private final Accounts accounts;
    private final Debts debts;
    private final UserProfiles userProfiles;
    private final CurrentUser currentUser;
    private final Clock clock;

    SummaryController(
            Transactions transactions, Accounts accounts, Debts debts, UserProfiles userProfiles, CurrentUser currentUser,
            Clock clock) {
        this.transactions = transactions;
        this.accounts = accounts;
        this.debts = debts;
        this.userProfiles = userProfiles;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @GetMapping("/today-and-month")
    TodayAndMonthResponse todayAndMonth() {
        UUID userId = currentUser.requireUserId();
        ZoneId zone = userProfiles.zoneOf(userId);

        Transactions.TodayAndMonth summary = transactions.summarize(userId, zone);
        YearMonth thisMonth = YearMonth.from(LocalDate.now(clock.withZone(zone)));
        Debts.MonthDebtTotals debtTotals = debts.monthTotals(userId, zone, thisMonth);
        List<AccountBalanceResponse> balances = accounts.listForUser(userId).stream()
                .map(view -> new AccountBalanceResponse(view.id(), view.name(), view.balance()))
                .toList();

        List<ExpenseLineResponse> expenses = summary.todayExpenses().stream()
                .map(line -> new ExpenseLineResponse(
                        line.id(), line.accountId(), line.categoryId(), line.amount(), line.description(), line.transactionDate()))
                .toList();

        return new TodayAndMonthResponse(
                new TodaySummary(expenses, summary.todayTotal()),
                new MonthSummary(
                        summary.monthIncome(), summary.monthExpense(), summary.monthNet(), summary.monthDebtPayments(),
                        debtTotals.principal(), debtTotals.interestAndFee()),
                balances);
    }

    record ExpenseLineResponse(
            UUID id, UUID accountId, UUID categoryId, BigDecimal amount, String description, Instant transactionDate) {
    }

    record TodaySummary(List<ExpenseLineResponse> expenses, BigDecimal total) {
    }

    record MonthSummary(
            BigDecimal income, BigDecimal expense, BigDecimal net, BigDecimal debtPayments,
            BigDecimal debtPrincipal, BigDecimal debtInterestAndFee) {
    }

    record AccountBalanceResponse(UUID accountId, String accountName, BigDecimal balance) {
    }

    record TodayAndMonthResponse(TodaySummary today, MonthSummary month, List<AccountBalanceResponse> balances) {
    }

}
