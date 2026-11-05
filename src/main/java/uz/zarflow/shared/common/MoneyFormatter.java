package uz.zarflow.shared.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.springframework.stereotype.Component;

/**
 * Display formatting per docs/brand.md §1.1: space-grouped thousands, comma decimal,
 * ",00" hidden when the amount is a whole number. Short form only implements the
 * million tier from the brand spec ("8,5 mln") — smaller amounts just use {@link #format}.
 */
@Component
public class MoneyFormatter {

    private static final char GROUPING_SEPARATOR = ' ';
    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);

    public String format(Money money, Locale locale) {
        BigDecimal amount = money.amount();
        BigDecimal centsPart = amount.remainder(BigDecimal.ONE).abs();

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator(GROUPING_SEPARATOR);
        DecimalFormat wholeFormat = new DecimalFormat("#,##0", symbols);

        StringBuilder result = new StringBuilder(wholeFormat.format(amount.setScale(0, RoundingMode.DOWN)));
        if (centsPart.signum() != 0) {
            int cents = centsPart.movePointRight(2).setScale(0, RoundingMode.HALF_EVEN).intValueExact();
            result.append(',').append(cents < 10 ? "0" + cents : String.valueOf(cents));
        }
        result.append(' ').append(unit(locale));
        return result.toString();
    }

    public String formatShort(Money money, Locale locale) {
        BigDecimal amount = money.amount();
        if (amount.abs().compareTo(MILLION) < 0) {
            return format(money, locale);
        }
        BigDecimal millions = amount.divide(MILLION, 1, RoundingMode.HALF_EVEN);
        String text = millions.toPlainString().replace('.', ',');
        return text + " " + (isRussian(locale) ? "млн" : "mln");
    }

    private String unit(Locale locale) {
        return isRussian(locale) ? "сум" : "so'm";
    }

    private boolean isRussian(Locale locale) {
        return locale != null && "ru".equals(locale.getLanguage());
    }

}
