package uz.zarflow.humo;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Normalizes a raw HUMO transaction merchant string into a stable key for dedupe keywords,
 * P2P/loan detection, and category-memory lookups (docs/PRD.md §2/§4). Pure logic, no Spring.
 */
final class MerchantNormalizer {

    private static final Pattern CODE_WORDS = Pattern.compile("\\b(OOO|MCHJ|LLC|AJ)\\b");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    private static final List<String> P2P_TOKENS =
            List.of("P2P", "HUMO2VIS", "HUMO2UZCARD", "S UZCARD", "S HUMO");
    private static final List<String> LOAN_TOKENS = List.of("CREDIT", "KREDIT");

    private static final List<CategoryHint> CATEGORY_HINTS = List.of(
            new CategoryHint("shopping", List.of("UZUM")),
            new CategoryHint("transport", List.of("WB TAXI", "YANDEX GO", "MYTAXI", "TRANSPORT TOLOV")),
            new CategoryHint("food", List.of("KORZINKA", "MAKRO", "HAVAS")),
            new CategoryHint("cafe", List.of("EVOS", "KFC", "OQTEPA")),
            new CategoryHint("telecom", List.of("UCELL", "BEELINE", "UZMOBILE", "MOBIUZ")));

    private MerchantNormalizer() {
    }

    /** Uppercase; strip OOO/MCHJ/LLC/AJ tokens; cut everything from '>'; strip a trailing
     * " UZ"; collapse whitespace. E.g. "MCHJ ESTELINE>TASHKE" -&gt; "ESTELINE". */
    static String normalize(String merchantRaw) {
        String upper = merchantRaw.strip().toUpperCase();
        String stripped = CODE_WORDS.matcher(upper).replaceAll("");
        int cut = stripped.indexOf('>');
        String beforeCut = cut >= 0 ? stripped.substring(0, cut) : stripped;
        String withoutUz = beforeCut.endsWith(" UZ") ? beforeCut.substring(0, beforeCut.length() - 3) : beforeCut;
        return MULTI_SPACE.matcher(withoutUz).replaceAll(" ").strip();
    }

    static boolean isP2p(String merchantKey) {
        return P2P_TOKENS.stream().anyMatch(merchantKey::contains);
    }

    static boolean isLoan(String merchantKey) {
        return LOAN_TOKENS.stream().anyMatch(merchantKey::contains);
    }

    /** The system category code to try first for an unmapped merchant (docs/PRD.md §4 hints),
     * before falling back to asking the user. */
    static Optional<String> categoryHintCode(String merchantKey) {
        return CATEGORY_HINTS.stream()
                .filter(hint -> hint.keywords().stream().anyMatch(merchantKey::contains))
                .map(CategoryHint::categoryCode)
                .findFirst();
    }

    private record CategoryHint(String categoryCode, List<String> keywords) {
    }

}
