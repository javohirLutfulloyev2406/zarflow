package uz.zarflow.shared.common;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;

/**
 * uz/ru day-and-month labels for bot messages (e.g. "7-noyabr" / "7 ноября"), per
 * docs/brand.md. Not backed by {@code java.time} locale data because the JDK has no
 * "uz" locale with month names baked in.
 */
public final class MonthNames {

    private static final String[] UZ = {
            "yanvar", "fevral", "mart", "aprel", "may", "iyun",
            "iyul", "avgust", "sentyabr", "oktyabr", "noyabr", "dekabr"
    };

    /** Genitive case, for "7 ноября" -- day-and-month phrasing. */
    private static final String[] RU_GENITIVE = {
            "января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"
    };

    /** Nominative case, for a standalone "Ноябрь 2026" header. */
    private static final String[] RU_NOMINATIVE = {
            "январь", "февраль", "март", "апрель", "май", "июнь",
            "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь"
    };

    private MonthNames() {
    }

    public static String dayAndMonth(LocalDate date, Locale locale) {
        int index = date.getMonthValue() - 1;
        if (isRussian(locale)) {
            return date.getDayOfMonth() + " " + RU_GENITIVE[index];
        }
        return date.getDayOfMonth() + "-" + UZ[index];
    }

    public static String monthAndYear(YearMonth month, Locale locale) {
        int index = month.getMonthValue() - 1;
        String name = isRussian(locale) ? RU_NOMINATIVE[index] : UZ[index];
        return capitalize(name) + " " + month.getYear();
    }

    private static String capitalize(String value) {
        return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static boolean isRussian(Locale locale) {
        return locale != null && "ru".equals(locale.getLanguage());
    }

}
