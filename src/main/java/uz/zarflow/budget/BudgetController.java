package uz.zarflow.budget;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import uz.zarflow.category.Categories;
import uz.zarflow.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/budgets")
class BudgetController {

    private final Budgets budgets;
    private final Categories categories;
    private final CurrentUser currentUser;

    BudgetController(Budgets budgets, Categories categories, CurrentUser currentUser) {
        this.budgets = budgets;
        this.categories = categories;
        this.currentUser = currentUser;
    }

    @GetMapping("/today")
    TodayResponse today() {
        UUID userId = currentUser.requireUserId();
        Map<UUID, Categories.CategoryView> categoryById = categoryById(userId);
        return budgets.getToday(userId)
                .map(v -> toTodayResponse(v, categoryById))
                .orElseGet(() -> new TodayResponse(
                        false, null, null, null, false, null, null, null, false, null, false, List.of()));
    }

    @GetMapping("/{month}")
    BudgetResponse get(@PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        UUID userId = currentUser.requireUserId();
        Map<UUID, Categories.CategoryView> categoryById = categoryById(userId);
        return budgets.getBudget(userId, month)
                .map(v -> toBudgetResponse(v, categoryById))
                .orElseGet(() -> new BudgetResponse(false, month.toString(), null, null, true, null, true, null, null, List.of()));
    }

    @PutMapping("/{month}")
    BudgetResponse update(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month, @Valid @RequestBody UpdateBudgetRequest request) {
        UUID userId = currentUser.requireUserId();
        Budgets.BudgetUpdate update = new Budgets.BudgetUpdate(
                request.expectedIncome(), request.fixedPayments(), request.loanPayments(), request.plannedSavings(),
                request.limits().stream().map(l -> new Budgets.LimitUpdate(l.categoryId(), l.amount())).toList());
        Budgets.BudgetView view = budgets.updateBudget(userId, month, update);
        return toBudgetResponse(view, categoryById(userId));
    }

    private Map<UUID, Categories.CategoryView> categoryById(UUID userId) {
        return categories.listForUser(userId).stream().collect(Collectors.toMap(Categories.CategoryView::id, c -> c));
    }

    private boolean isRussian() {
        return "ru".equals(LocaleContextHolder.getLocale().getLanguage());
    }

    private LimitResponse toLimitResponse(Budgets.LimitView view, Map<UUID, Categories.CategoryView> categoryById) {
        Categories.CategoryView category = categoryById.get(view.categoryId());
        boolean russian = isRussian();
        return new LimitResponse(
                view.categoryId(),
                category == null ? "" : (russian ? category.nameRu() : category.nameUz()),
                category == null ? null : category.color(),
                category == null ? null : category.icon(),
                view.amount(),
                view.spent(),
                view.percent());
    }

    private BudgetResponse toBudgetResponse(Budgets.BudgetView view, Map<UUID, Categories.CategoryView> categoryById) {
        return new BudgetResponse(
                true, view.month().toString(), view.expectedIncome(),
                view.fixedPayments(), view.fixedPaymentsIsAuto(), view.loanPayments(), view.loanPaymentsIsAuto(),
                view.plannedSavings(), view.freeBudget(),
                view.limits().stream().map(l -> toLimitResponse(l, categoryById)).toList());
    }

    private TodayResponse toTodayResponse(Budgets.TodayView view, Map<UUID, Categories.CategoryView> categoryById) {
        return new TodayResponse(
                true, view.dailyAllowance(), view.spentToday(), view.todayLeft(), view.overspentToday(),
                view.overspentAmount(), view.tomorrowAllowance(), view.freeBudget(), view.obligationsExceedIncome(),
                view.forecastOverage(), view.forecastPositive(),
                view.limits().stream().map(l -> toLimitResponse(l, categoryById)).toList());
    }

    record LimitResponse(
            UUID categoryId, String categoryName, String categoryColor, String categoryIcon,
            BigDecimal amount, BigDecimal spent, BigDecimal percent) {
    }

    record BudgetResponse(
            boolean hasBudget, String month, BigDecimal expectedIncome,
            BigDecimal fixedPayments, boolean fixedPaymentsIsAuto,
            BigDecimal loanPayments, boolean loanPaymentsIsAuto,
            BigDecimal plannedSavings, BigDecimal freeBudget, List<LimitResponse> limits) {
    }

    record TodayResponse(
            boolean hasBudget, BigDecimal dailyAllowance, BigDecimal spentToday, BigDecimal todayLeft,
            boolean overspentToday, BigDecimal overspentAmount, BigDecimal tomorrowAllowance, BigDecimal freeBudget,
            boolean obligationsExceedIncome, BigDecimal forecastOverage, boolean forecastPositive,
            List<LimitResponse> limits) {
    }

    record UpdateLimitRequest(
            @NotNull UUID categoryId,
            @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 12, fraction = 2) BigDecimal amount) {
    }

    /** {@code fixedPayments}/{@code loanPayments} are optional: omitted/null means "auto from
     * recurring rules"; a value sets a manual override. */
    record UpdateBudgetRequest(
            @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal expectedIncome,
            @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal fixedPayments,
            @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal loanPayments,
            @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal plannedSavings,
            @NotNull List<@Valid UpdateLimitRequest> limits) {
    }

}
