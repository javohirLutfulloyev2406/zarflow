package uz.zarflow.insight;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import uz.zarflow.category.Categories;
import uz.zarflow.shared.common.Money;
import uz.zarflow.shared.common.MoneyFormatter;

/** Renders a stored insight's JSON payload into the one-sentence bot/mini-app text, in
 * either language. The same message keys serve both surfaces. Category names are
 * resolved fresh at render time (never baked into the stored payload) so the same
 * insight renders correctly however the reader's locale is decided. */
@Component
class InsightTextRenderer {

    private final MessageSource messageSource;
    private final MoneyFormatter moneyFormatter;
    private final Categories categories;

    InsightTextRenderer(MessageSource messageSource, MoneyFormatter moneyFormatter, Categories categories) {
        this.messageSource = messageSource;
        this.moneyFormatter = moneyFormatter;
        this.categories = categories;
    }

    String icon(InsightCode code) {
        return switch (code) {
            case CATEGORY_UP_WEEK -> "📈";
            case BUDGET_80 -> "⚠️";
            case FORECAST_OVER -> "📉";
            case DEBT_SHARE -> "💳";
            case DISCRETIONARY_DOWN -> "🎉";
            case NO_ENTRIES_3_DAYS -> "✍️";
        };
    }

    boolean actionable(InsightCode code) {
        return switch (code) {
            case CATEGORY_UP_WEEK, BUDGET_80, FORECAST_OVER, NO_ENTRIES_3_DAYS -> true;
            case DEBT_SHARE, DISCRETIONARY_DOWN -> false;
        };
    }

    String render(UUID userId, InsightCode code, Map<String, String> payload, Locale locale) {
        String money = switch (code) {
            case CATEGORY_UP_WEEK -> moneyFormatter.format(Money.of(new BigDecimal(payload.get("diffAmount"))), locale);
            case FORECAST_OVER -> moneyFormatter.format(Money.of(new BigDecimal(payload.get("overageAmount"))), locale);
            case DISCRETIONARY_DOWN -> moneyFormatter.format(Money.of(new BigDecimal(payload.get("diffAmount"))), locale);
            default -> null;
        };
        return switch (code) {
            case CATEGORY_UP_WEEK -> messageSource.getMessage(
                    "insight.category-up-week",
                    new Object[]{categoryName(userId, payload.get("categoryId"), locale), payload.get("percent"), money},
                    locale);
            case BUDGET_80 -> messageSource.getMessage(
                    "insight.budget-80", new Object[]{payload.get("percentUsed"), payload.get("daysLeft")}, locale);
            case FORECAST_OVER -> messageSource.getMessage("insight.forecast-over", new Object[]{money}, locale);
            case DEBT_SHARE -> messageSource.getMessage("insight.debt-share", new Object[]{payload.get("percent")}, locale);
            case DISCRETIONARY_DOWN -> messageSource.getMessage("insight.discretionary-down", new Object[]{money}, locale);
            case NO_ENTRIES_3_DAYS -> messageSource.getMessage("insight.no-entries-3-days", new Object[]{payload.get("days")}, locale);
        };
    }

    private String categoryName(UUID userId, String categoryId, Locale locale) {
        boolean russian = "ru".equals(locale.getLanguage());
        return categories.listForUser(userId).stream()
                .filter(c -> c.id().toString().equals(categoryId))
                .findFirst()
                .map(c -> russian ? c.nameRu() : c.nameUz())
                .orElse("");
    }

}
