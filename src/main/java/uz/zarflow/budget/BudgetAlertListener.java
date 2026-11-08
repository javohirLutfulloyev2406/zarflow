package uz.zarflow.budget;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import uz.zarflow.transaction.Transactions;

/**
 * Reacts to a committed expense save by checking budget-limit thresholds. Listening
 * (rather than {@code transaction} calling into {@code budget} directly) keeps the
 * module dependency one-directional: {@code budget -> transaction}, never the reverse.
 */
@Component
class BudgetAlertListener {

    private final BudgetAlertService budgetAlertService;

    BudgetAlertListener(BudgetAlertService budgetAlertService) {
        this.budgetAlertService = budgetAlertService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onExpenseRecorded(Transactions.ExpenseRecorded event) {
        budgetAlertService.checkAndNotify(event.userId(), event.categoryId());
    }

}
