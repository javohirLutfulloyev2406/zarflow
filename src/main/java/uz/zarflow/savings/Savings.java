package uz.zarflow.savings;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The savings module's public API: goals, contributions/withdrawals, and month-end
 * leftover routing. Other modules talk to savings only through here — never through the
 * entities/repositories.
 */
public interface Savings {

    List<GoalView> listForUser(UUID userId);

    GoalView get(UUID userId, UUID goalId);

    GoalView create(UUID userId, CreateGoal input);

    GoalView update(UUID userId, UUID goalId, UpdateGoal input);

    GoalView archive(UUID userId, UUID goalId);

    /** A TRANSFER from {@code fromAccountId} into the goal's own SAVINGS account — never
     * income or expense. Reaching target closes the goal (ACHIEVED) and may unlock FIRST_GOAL. */
    ContributionResult contribute(UUID userId, UUID goalId, UUID fromAccountId, BigDecimal amount);

    /** The reverse transfer — out of the goal's account into {@code toAccountId}. */
    ContributionResult withdraw(UUID userId, UUID goalId, UUID toAccountId, BigDecimal amount);

    List<ContributionView> history(UUID userId, UUID goalId);

    /** One month of obligations (budget fixed + loan payments) — the emergency-fund template
     * target. Empty once the user already has an active emergency goal. */
    Optional<BigDecimal> suggestedEmergencyTarget(UUID userId);

    /** Runs once per month (called alongside the monthly report): upserts an OFFERED
     * leftover_offers row for {@code month} if the free budget wasn't fully spent, and
     * publishes {@link LeftoverOffered}. A no-op if an offer already exists for that month. */
    void evaluateLeftover(UUID userId, YearMonth month);

    /** The most recent still-{@code OFFERED} leftover offer, if any. */
    Optional<LeftoverOfferView> currentOffer(UUID userId);

    /** Idempotent: resolving an already-resolved offer just returns its current state. */
    LeftoverOfferView resolve(UUID userId, YearMonth month, Resolution resolution);

    /** This local month's total saved across all goals, and that as a % of month income --
     * the monthly report's "Jamg'arma: X so'm (daromadning Y%)" line. */
    MonthSavingsSummary monthSummary(UUID userId, ZoneId zone, YearMonth month);

    record CreateGoal(String name, BigDecimal targetAmount, LocalDate targetDate, UUID accountId, boolean isEmergency) {
    }

    record UpdateGoal(String name, BigDecimal targetAmount, LocalDate targetDate) {
    }

    record GoalView(
            UUID id, String name, BigDecimal targetAmount, BigDecimal savedAmount, LocalDate targetDate, UUID accountId,
            boolean isEmergency, GoalStatus status, BigDecimal remaining, BigDecimal perMonth, BigDecimal perWeek,
            BigDecimal pace, LocalDate forecastDate, boolean onTrack) {
    }

    record ContributionResult(UUID goalId, BigDecimal savedAmount, boolean achieved) {
    }

    record ContributionView(UUID id, Instant date, BigDecimal amount) {
    }

    enum ResolutionType { TO_GOAL, TO_DEBT, SPLIT, DISMISSED }

    /** {@code splitGoalAmount}/{@code splitDebtAmount} only matter for {@code SPLIT}. */
    record Resolution(ResolutionType type, BigDecimal splitGoalAmount, BigDecimal splitDebtAmount) {
    }

    record LeftoverOfferView(UUID id, YearMonth month, BigDecimal amount, LeftoverStatus status) {
    }

    record MonthSavingsSummary(BigDecimal totalSaved, BigDecimal percentOfIncome) {
    }

    /** Published when a goal reaches target — the telegram module sends a congratulation,
     * and (if {@code firstGoalAchievementUnlocked}) the FIRST_GOAL achievement message too. */
    record GoalAchieved(UUID userId, UUID goalId, String goalName, boolean firstGoalAchievementUnlocked) {
    }

    /** Published once a positive leftover offer is created for a month — the telegram
     * module sends the routing-choice message. */
    record LeftoverOffered(UUID userId, YearMonth month, BigDecimal amount) {
    }

}
