package uz.zarflow.budget;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import uz.zarflow.shared.common.Percentages;
import uz.zarflow.transaction.TransactionType;
import uz.zarflow.transaction.Transactions;
import uz.zarflow.user.UserProfiles;

/** Checks whether a just-saved expense pushed its category past 80%/100% of its budget limit. */
@Service
class BudgetAlertService {

    private static final BigDecimal EIGHTY = BigDecimal.valueOf(80);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BudgetRepository budgetRepository;
    private final BudgetLimitRepository budgetLimitRepository;
    private final Transactions transactions;
    private final UserProfiles userProfiles;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    BudgetAlertService(
            BudgetRepository budgetRepository,
            BudgetLimitRepository budgetLimitRepository,
            Transactions transactions,
            UserProfiles userProfiles,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.budgetRepository = budgetRepository;
        this.budgetLimitRepository = budgetLimitRepository;
        this.transactions = transactions;
        this.userProfiles = userProfiles;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /** Public: proxy-based {@code @Transactional} AOP only intercepts public methods. REQUIRES_NEW:
     * called from an AFTER_COMMIT event listener, where the triggering transaction's resources are
     * still technically bound to the thread during its own completion callbacks -- joining it with
     * the default REQUIRED propagation silently no-ops instead of starting a real, independently
     * committed transaction. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkAndNotify(UUID userId, UUID categoryId) {
        if (categoryId == null) {
            return;
        }
        ZoneId zone = userProfiles.zoneOf(userId);
        YearMonth month = YearMonth.from(LocalDate.now(clock.withZone(zone)));

        Budget budget = budgetRepository.findByUserIdAndMonth(userId, month.atDay(1)).orElse(null);
        if (budget == null) {
            return;
        }
        BudgetLimit limit = budgetLimitRepository.findByBudgetIdAndCategoryId(budget.getId(), categoryId).orElse(null);
        if (limit == null || limit.getAmount().signum() == 0) {
            return;
        }

        BigDecimal spent = categorySpend(userId, zone, month, categoryId);
        BigDecimal percent = Percentages.shareOf(spent, limit.getAmount());

        if (percent.compareTo(HUNDRED) >= 0 && !limit.isNotified100()) {
            limit.markNotified100();
            budgetLimitRepository.save(limit);
            eventPublisher.publishEvent(new Budgets.LimitThresholdCrossed(userId, categoryId, 100));
        } else if (percent.compareTo(EIGHTY) >= 0 && !limit.isNotified80()) {
            limit.markNotified80();
            budgetLimitRepository.save(limit);
            eventPublisher.publishEvent(new Budgets.LimitThresholdCrossed(userId, categoryId, 80));
        }
    }

    private BigDecimal categorySpend(UUID userId, ZoneId zone, YearMonth month, UUID categoryId) {
        List<Transactions.MonthEntry> entries = transactions.monthEntries(userId, zone, month);
        return entries.stream()
                .filter(e -> e.type() == TransactionType.EXPENSE && categoryId.equals(e.categoryId()))
                .map(Transactions.MonthEntry::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
