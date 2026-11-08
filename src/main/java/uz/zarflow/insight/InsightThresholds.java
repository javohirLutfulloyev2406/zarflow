package uz.zarflow.insight;

import java.math.BigDecimal;

/** Every tunable number the rule engine checks against, in one place. */
final class InsightThresholds {

    static final BigDecimal CATEGORY_UP_PERCENT = BigDecimal.valueOf(30);
    static final BigDecimal CATEGORY_UP_MIN_AMOUNT = BigDecimal.valueOf(50_000);

    static final BigDecimal BUDGET_80_PERCENT = BigDecimal.valueOf(80);
    static final long BUDGET_80_MIN_DAYS_LEFT = 5;

    static final BigDecimal DEBT_SHARE_PERCENT = BigDecimal.valueOf(20);

    static final int NO_ENTRIES_DAYS = 3;

    private InsightThresholds() {
    }

}
