package uz.zarflow.shared.common;

import java.time.LocalDate;
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

    private static final String[] RU = {
            "января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"
    };

    private MonthNames() {
    }

    public static String dayAndMonth(LocalDate date, Locale locale) {
        int index = date.getMonthValue() - 1;
        if (isRussian(locale)) {
            return date.getDayOfMonth() + " " + RU[index];
        }
        return date.getDayOfMonth() + "-" + UZ[index];
    }

    private static boolean isRussian(Locale locale) {
        return locale != null && "ru".equals(locale.getLanguage());
    }

}
