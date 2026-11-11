package uz.zarflow.humo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a HUMO balance message (forwarded from @HUMOcardbot, or pasted) into one entry
 * per card block (docs/PRD.md §7). Pure logic, no Spring -- trivially unit-testable.
 *
 * <p>Amount format here is apostrophe-grouped thousands, dot decimal ({@code 109'151.58},
 * {@code 109’151.58}, or plain {@code 0.00}/{@code 623.47} with no grouping at all) --
 * notably different from the bot's own quick-input amount parsing ({@link
 * uz.zarflow.telegram} package), which is comma/dot-grouped with no apostrophe handling.
 */
final class HumoBalanceParser {

    private static final Pattern CARD_PATTERN = Pattern.compile("(?i)humo\\s*card\\s*\\*\\s*(\\d{4})");
    private static final Pattern AMOUNT_PATTERN =
            Pattern.compile("([0-9][0-9.,'‘’\\s]*[0-9]|[0-9])\\s*uzs", Pattern.CASE_INSENSITIVE);
    private static final Pattern THOUSANDS_SEPARATOR = Pattern.compile("['‘’\\s]");

    private HumoBalanceParser() {
    }

    /** One entry per recognized "HUMOCARD *XXXX ... <amount> UZS" block, in message order.
     * Empty when the text has no recognizable card block at all (not a Humo balance message). */
    static List<HumoCardAmount> parse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return List.of();
        }
        Matcher cardMatcher = CARD_PATTERN.matcher(rawText);
        List<Integer> blockStarts = new ArrayList<>();
        List<Integer> headerEnds = new ArrayList<>();
        List<String> last4s = new ArrayList<>();
        while (cardMatcher.find()) {
            blockStarts.add(cardMatcher.start());
            headerEnds.add(cardMatcher.end());
            last4s.add(cardMatcher.group(1));
        }
        if (blockStarts.isEmpty()) {
            return List.of();
        }

        List<HumoCardAmount> results = new ArrayList<>();
        for (int i = 0; i < blockStarts.size(); i++) {
            int blockEnd = i + 1 < blockStarts.size() ? blockStarts.get(i + 1) : rawText.length();
            String block = rawText.substring(headerEnds.get(i), blockEnd);
            Matcher amountMatcher = AMOUNT_PATTERN.matcher(block);
            if (amountMatcher.find()) {
                results.add(new HumoCardAmount(last4s.get(i), parseAmount(amountMatcher.group(1))));
            }
        }
        return results;
    }

    private static BigDecimal parseAmount(String raw) {
        String cleaned = THOUSANDS_SEPARATOR.matcher(raw.strip()).replaceAll("");
        return new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_EVEN);
    }

    /** A parsed card block: its last-4-digit label and the bank balance it reported. */
    record HumoCardAmount(String cardLast4, BigDecimal balance) {
    }

}