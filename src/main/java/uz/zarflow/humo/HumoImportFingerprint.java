package uz.zarflow.humo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

import uz.zarflow.humo.HumoTransactionParser.HumoTransactionBlock;
import uz.zarflow.transaction.TransactionType;

/**
 * Dedupe key for an imported HUMO transaction block (docs/PRD.md §5):
 * {@code sha256("HUMO|last4|sign|amount|yyyy-MM-ddTHH:mm|merchantRaw")}. Pure logic, no
 * Spring -- the DB-level claim ({@code bank_imports}, unique on user+fingerprint) is just
 * glue code around this.
 */
final class HumoImportFingerprint {

    private static final ZoneId TASHKENT = ZoneId.of("Asia/Tashkent");
    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private HumoImportFingerprint() {
    }

    static String of(HumoTransactionBlock block) {
        String sign = block.type() == TransactionType.INCOME ? "+" : "-";
        String minute = block.transactionDate().atZone(TASHKENT).format(MINUTE);
        String joined = String.join(
                "|", "HUMO", block.cardLast4(), sign, block.amount().toPlainString(), minute, block.merchantRaw());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(joined.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

}
