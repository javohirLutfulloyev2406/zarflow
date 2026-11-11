package uz.zarflow.savings;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import uz.zarflow.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/savings-goals")
class SavingsGoalController {

    private final Savings savings;
    private final CurrentUser currentUser;

    SavingsGoalController(Savings savings, CurrentUser currentUser) {
        this.savings = savings;
        this.currentUser = currentUser;
    }

    @GetMapping
    List<GoalResponse> list() {
        return savings.listForUser(currentUser.requireUserId()).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    GoalResponse get(@PathVariable UUID id) {
        return toResponse(savings.get(currentUser.requireUserId(), id));
    }

    @GetMapping("/emergency-target")
    EmergencyTargetResponse emergencyTarget() {
        Optional<BigDecimal> target = savings.suggestedEmergencyTarget(currentUser.requireUserId());
        return new EmergencyTargetResponse(target.orElse(null));
    }

    @PostMapping
    GoalResponse create(@Valid @RequestBody CreateGoalRequest request) {
        Savings.CreateGoal input = new Savings.CreateGoal(
                request.name(), request.targetAmount(), request.targetDate(), request.accountId(), request.isEmergency());
        return toResponse(savings.create(currentUser.requireUserId(), input));
    }

    @PutMapping("/{id}")
    GoalResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateGoalRequest request) {
        Savings.UpdateGoal input = new Savings.UpdateGoal(request.name(), request.targetAmount(), request.targetDate());
        return toResponse(savings.update(currentUser.requireUserId(), id, input));
    }

    @PostMapping("/{id}/archive")
    GoalResponse archive(@PathVariable UUID id) {
        return toResponse(savings.archive(currentUser.requireUserId(), id));
    }

    @PostMapping("/{id}/contributions")
    ContributionResponse contribute(@PathVariable UUID id, @Valid @RequestBody ContributionRequest request) {
        Savings.ContributionResult result = savings.contribute(
                currentUser.requireUserId(), id, request.fromAccountId(), request.amount());
        return toContributionResponse(result);
    }

    @PostMapping("/{id}/withdrawals")
    ContributionResponse withdraw(@PathVariable UUID id, @Valid @RequestBody WithdrawalRequest request) {
        Savings.ContributionResult result = savings.withdraw(
                currentUser.requireUserId(), id, request.toAccountId(), request.amount());
        return toContributionResponse(result);
    }

    @GetMapping("/{id}/history")
    List<ContributionHistoryResponse> history(@PathVariable UUID id) {
        return savings.history(currentUser.requireUserId(), id).stream()
                .map(c -> new ContributionHistoryResponse(c.id(), c.date(), c.amount()))
                .toList();
    }

    private GoalResponse toResponse(Savings.GoalView view) {
        return new GoalResponse(
                view.id(), view.name(), view.targetAmount(), view.savedAmount(), view.targetDate(), view.accountId(),
                view.isEmergency(), view.status(), view.remaining(), view.perMonth(), view.perWeek(), view.pace(),
                view.forecastDate(), view.onTrack());
    }

    private ContributionResponse toContributionResponse(Savings.ContributionResult result) {
        return new ContributionResponse(result.goalId(), result.savedAmount(), result.achieved());
    }

    record CreateGoalRequest(
            @NotBlank String name,
            @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 12, fraction = 2) BigDecimal targetAmount,
            LocalDate targetDate,
            @NotNull UUID accountId,
            boolean isEmergency) {
    }

    record UpdateGoalRequest(
            @NotBlank String name,
            @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 12, fraction = 2) BigDecimal targetAmount,
            LocalDate targetDate) {
    }

    record ContributionRequest(
            @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 12, fraction = 2) BigDecimal amount,
            @NotNull UUID fromAccountId) {
    }

    record WithdrawalRequest(
            @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 12, fraction = 2) BigDecimal amount,
            @NotNull UUID toAccountId) {
    }

    record GoalResponse(
            UUID id, String name, BigDecimal targetAmount, BigDecimal savedAmount, LocalDate targetDate, UUID accountId,
            boolean isEmergency, GoalStatus status, BigDecimal remaining, BigDecimal perMonth, BigDecimal perWeek,
            BigDecimal pace, LocalDate forecastDate, boolean onTrack) {
    }

    record ContributionResponse(UUID goalId, BigDecimal savedAmount, boolean achieved) {
    }

    record ContributionHistoryResponse(UUID id, java.time.Instant date, BigDecimal amount) {
    }

    record EmergencyTargetResponse(BigDecimal suggestedTarget) {
    }

}
