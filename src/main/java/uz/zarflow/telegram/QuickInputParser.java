package uz.zarflow.telegram;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uz.zarflow.category.CategoryType;
import uz.zarflow.transaction.TransactionType;

/**
 * Parses the bot's free-text quick-input lines (docs/PRD.md §5) into a transaction
 * draft. Pure logic, no Spring/JPA — categories and the user's learned phrase-&gt;category
 * memory are passed in as plain data so this stays trivially unit-testable.
 */
final class QuickInputParser {

    private static final Set<String> INCOME_KEYWORDS =
            Set.of("oylik", "maosh", "avans", "bonus", "зарплата", "аванс");

    private static final Set<String> CASH_KEYWORDS = Set.of("naqd", "нал");
    private static final Set<String> CARD_KEYWORDS = Set.of("karta", "humo", "uzcard");

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("\\+?\\d[\\d.,\\s]*\\d|\\+?\\d");
    private static final Pattern SUFFIX_PATTERN = Pattern.compile(
            "^(k|ming|mln|m|млн)\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern APOSTROPHES = Pattern.compile("['‘’`]");
    private static final Pattern WORD_SPLIT = Pattern.compile("[^\\p{L}\\p{N}]+");

    List<ParsedLine> parse(String message, List<CategoryKeyword> categories, Map<String, UUID> parsingMemory) {
        List<ParsedLine> results = new ArrayList<>();
        for (String rawLine : message.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            parseLine(line, categories, parsingMemory).ifPresent(results::add);
        }
        return results;
    }

    Optional<ParsedLine> parseLine(String line, List<CategoryKeyword> categories, Map<String, UUID> parsingMemory) {
        Matcher matcher = AMOUNT_PATTERN.matcher(line);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String numberRaw = matcher.group();
        boolean plusPrefix = numberRaw.startsWith("+");
        String digitsOnly = plusPrefix ? numberRaw.substring(1) : numberRaw;

        int afterNumber = matcher.end();
        String rest = line.substring(afterNumber).stripLeading();
        Matcher suffixMatcher = SUFFIX_PATTERN.matcher(rest);

        BigDecimal amount;
        int consumedEnd = matcher.end();
        if (suffixMatcher.find()) {
            BigDecimal base = parseDecimal(digitsOnly);
            BigDecimal factor = multiplierFor(suffixMatcher.group());
            amount = base.multiply(factor);
            consumedEnd = afterNumber + suffixMatcher.end();
        } else {
            amount = parseWholeNumber(digitsOnly);
        }
        amount = amount.setScale(2, RoundingMode.HALF_EVEN);
        if (amount.signum() <= 0) {
            return Optional.empty();
        }

        String noteWithoutAmount = (line.substring(0, matcher.start()) + " " + line.substring(consumedEnd)).trim();
        String phrase = normalizePhrase(noteWithoutAmount);
        List<String> words = tokenize(noteWithoutAmount);

        AccountHint accountHint = resolveAccountHint(words);
        CategoryMatch categoryMatch = resolveCategory(phrase, words, categories, parsingMemory);

        TransactionType type = resolveType(plusPrefix, words, categoryMatch);
        Confidence confidence = categoryMatch.categoryId() != null ? Confidence.HIGH : Confidence.LOW;

        return Optional.of(new ParsedLine(
                amount, type, categoryMatch.categoryId(), confidence, accountHint, phrase, line));
    }

    /** Same normalization the parser itself uses for keyword/phrase matching — callers persist and look up memory with this. */
    static String normalizePhrase(String text) {
        return normalize(text).trim();
    }

    private TransactionType resolveType(boolean plusPrefix, List<String> words, CategoryMatch categoryMatch) {
        if (plusPrefix) {
            return TransactionType.INCOME;
        }
        for (String word : words) {
            if (INCOME_KEYWORDS.contains(word)) {
                return TransactionType.INCOME;
            }
        }
        if (categoryMatch.type() == CategoryType.INCOME) {
            return TransactionType.INCOME;
        }
        return TransactionType.EXPENSE;
    }

    private AccountHint resolveAccountHint(List<String> words) {
        for (String word : words) {
            if (CASH_KEYWORDS.contains(word)) {
                return AccountHint.CASH;
            }
            if (CARD_KEYWORDS.contains(word)) {
                return AccountHint.BANK_CARD;
            }
        }
        return AccountHint.DEFAULT;
    }

    private CategoryMatch resolveCategory(
            String phrase, List<String> words, List<CategoryKeyword> categories, Map<String, UUID> parsingMemory) {
        UUID remembered = parsingMemory.get(phrase);
        if (remembered != null) {
            CategoryType type = categories.stream()
                    .filter(c -> c.categoryId().equals(remembered))
                    .map(CategoryKeyword::type)
                    .findFirst()
                    .orElse(null);
            return new CategoryMatch(remembered, type);
        }

        Map<String, CategoryKeyword> byKeyword = new HashMap<>();
        for (CategoryKeyword category : categories) {
            for (String keyword : category.keywords()) {
                byKeyword.putIfAbsent(normalize(keyword), category);
            }
        }
        for (String word : words) {
            CategoryKeyword match = byKeyword.get(word);
            if (match != null) {
                return new CategoryMatch(match.categoryId(), match.type());
            }
        }
        return new CategoryMatch(null, null);
    }

    private List<String> tokenize(String text) {
        List<String> words = new ArrayList<>();
        for (String word : WORD_SPLIT.split(normalize(text))) {
            if (!word.isBlank()) {
                words.add(word);
            }
        }
        return words;
    }

    private static String normalize(String text) {
        return APOSTROPHES.matcher(text.toLowerCase(Locale.ROOT)).replaceAll("");
    }

    private BigDecimal parseDecimal(String raw) {
        String cleaned = raw.strip().replace(" ", "").replace(",", ".");
        return new BigDecimal(cleaned);
    }

    private BigDecimal parseWholeNumber(String raw) {
        String cleaned = raw.replaceAll("[\\s.,]", "");
        return new BigDecimal(cleaned);
    }

    private BigDecimal multiplierFor(String suffix) {
        return switch (suffix.toLowerCase(Locale.ROOT)) {
            case "k", "ming" -> BigDecimal.valueOf(1_000);
            case "mln", "m", "млн" -> BigDecimal.valueOf(1_000_000);
            default -> throw new IllegalStateException("Unreachable: unmatched suffix " + suffix);
        };
    }

    private record CategoryMatch(UUID categoryId, CategoryType type) {
    }

    record CategoryKeyword(UUID categoryId, CategoryType type, List<String> keywords) {
    }

    enum AccountHint { CASH, BANK_CARD, DEFAULT }

    enum Confidence { HIGH, LOW }

    record ParsedLine(
            BigDecimal amount, TransactionType type, UUID categoryId, Confidence confidence,
            AccountHint accountHint, String notePhrase, String rawLine) {
    }

}
