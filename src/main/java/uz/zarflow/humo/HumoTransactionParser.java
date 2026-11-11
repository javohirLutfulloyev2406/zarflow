package uz.zarflow.humo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uz.zarflow.transaction.TransactionType;

/**
 * Parses HUMO card transaction notifications (forwarded from @HUMOcardbot, or pasted,
 * possibly several blocks in one message) into one entry per 5-line block (docs/PRD.md
 * §2). Pure logic, no Spring -- trivially unit-testable.
 *
 * <p>Amount format here is dot-grouped thousands, comma decimal ({@code 54.300,00 UZS})
 * -- notably different from {@link HumoBalanceParser}'s apostrophe-grouped format.
 */
final class HumoTransactionParser {

    private static final ZoneId TASHKENT = ZoneId.of("Asia/Tashkent");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy");

    private static final Pattern TYPE_LINE = Pattern.compile("(?m)^.*(💸|🎉).*$");
    private static final Pattern SIGN_AMOUNT = Pattern.compile("([➖➕])\\s*([0-9.,]+)\\s*([A-Za-z]{3})");
    private static final Pattern MERCHANT_LINE = Pattern.compile("📍\\s*(.+)");
    private static final Pattern CARD_LINE = Pattern.compile("(?i)humocard\\s*\\*\\s*(\\d{4})");
    private static final Pattern TIME_LINE = Pattern.compile("(\\d{2}:\\d{2}\\s+\\d{2}\\.\\d{2}\\.\\d{4})");

    private HumoTransactionParser() {
    }

    /** One entry per recognized 5-line transaction block, in message order. Blocks whose
     * amount isn't UZS are counted in {@code skippedNonUzs} rather than returned. Empty
     * blocks (empty block list, zero skips) means "not a Humo transaction message at all". */
    static ParsedBatch parse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return new ParsedBatch(List.of(), 0);
        }
        Matcher typeMatcher = TYPE_LINE.matcher(rawText);
        List<Integer> starts = new ArrayList<>();
        while (typeMatcher.find()) {
            starts.add(typeMatcher.start());
        }
        if (starts.isEmpty()) {
            return new ParsedBatch(List.of(), 0);
        }

        List<HumoTransactionBlock> blocks = new ArrayList<>();
        int skippedNonUzs = 0;
        for (int i = 0; i < starts.size(); i++) {
            int end = i + 1 < starts.size() ? starts.get(i + 1) : rawText.length();
            String block = rawText.substring(starts.get(i), end);
            HumoTransactionBlock parsed = parseBlock(block);
            if (parsed == null) {
                continue;
            }
            if (parsed.currencyIfSkipped() != null) {
                skippedNonUzs++;
            } else {
                blocks.add(parsed);
            }
        }
        return new ParsedBatch(blocks, skippedNonUzs);
    }

    private static HumoTransactionBlock parseBlock(String block) {
        Matcher signAmount = SIGN_AMOUNT.matcher(block);
        Matcher merchant = MERCHANT_LINE.matcher(block);
        Matcher card = CARD_LINE.matcher(block);
        Matcher time = TIME_LINE.matcher(block);
        if (!signAmount.find() || !merchant.find() || !card.find() || !time.find()) {
            return null;
        }

        String currency = signAmount.group(3);
        if (!"UZS".equalsIgnoreCase(currency)) {
            return HumoTransactionBlock.skippedNonUzs(currency);
        }

        TransactionType type = "➕".equals(signAmount.group(1)) ? TransactionType.INCOME : TransactionType.EXPENSE;
        BigDecimal amount = parseAmount(signAmount.group(2));
        String merchantRaw = merchant.group(1).strip();
        String merchantKey = MerchantNormalizer.normalize(merchantRaw);
        String cardLast4 = card.group(1);
        Instant transactionDate = LocalDateTime.parse(time.group(1), TIME_FORMAT).atZone(TASHKENT).toInstant();

        return new HumoTransactionBlock(type, amount, merchantRaw, merchantKey, cardLast4, transactionDate, null);
    }

    /** Dot = thousands separator, comma = decimal separator ({@code 54.300,00} -&gt; 54300.00). */
    private static BigDecimal parseAmount(String raw) {
        String noThousands = raw.replace(".", "");
        String dotDecimal = noThousands.replace(",", ".");
        return new BigDecimal(dotDecimal).setScale(2, RoundingMode.HALF_EVEN);
    }

    record ParsedBatch(List<HumoTransactionBlock> blocks, int skippedNonUzs) {
    }

    /** A parsed transaction block. {@code currencyIfSkipped} is non-null only for the
     * sentinel instance representing a recognized-but-non-UZS block (every other field
     * is null/zero on that sentinel). */
    record HumoTransactionBlock(
            TransactionType type, BigDecimal amount, String merchantRaw, String merchantKey,
            String cardLast4, Instant transactionDate, String currencyIfSkipped) {

        static HumoTransactionBlock skippedNonUzs(String currency) {
            return new HumoTransactionBlock(null, null, null, null, null, null, currency);
        }
    }

}
