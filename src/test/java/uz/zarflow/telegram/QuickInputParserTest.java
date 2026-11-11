package uz.zarflow.telegram;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import uz.zarflow.category.CategoryType;
import uz.zarflow.transaction.TransactionType;

import static org.assertj.core.api.Assertions.assertThat;

class QuickInputParserTest {

    private static final UUID CAFE = UUID.randomUUID();
    private static final UUID TRANSPORT = UUID.randomUUID();
    private static final UUID SALARY = UUID.randomUUID();
    private static final UUID FREELANCE = UUID.randomUUID();

    private static final List<QuickInputParser.CategoryKeyword> CATEGORIES = List.of(
            new QuickInputParser.CategoryKeyword(CAFE, CategoryType.EXPENSE,
                    List.of("tushlik", "kafe", "kofe", "lavash", "osh", "обед", "кафе", "кофе")),
            new QuickInputParser.CategoryKeyword(TRANSPORT, CategoryType.EXPENSE,
                    List.of("taksi", "yandex", "metro", "avtobus", "benzin", "такси", "бензин")),
            new QuickInputParser.CategoryKeyword(SALARY, CategoryType.INCOME,
                    List.of("oylik", "maosh", "зарплата")),
            new QuickInputParser.CategoryKeyword(FREELANCE, CategoryType.INCOME,
                    List.of("frilans", "фриланс")));

    private final QuickInputParser parser = new QuickInputParser();

    private QuickInputParser.ParsedLine parseOne(String line) {
        return parser.parseLine(line, CATEGORIES, Map.of()).orElseThrow();
    }

    @Test
    void amountThenNote() {
        var result = parseOne("25000 tushlik");
        assertThat(result.amount()).isEqualByComparingTo("25000.00");
        assertThat(result.categoryId()).isEqualTo(CAFE);
        assertThat(result.type()).isEqualTo(TransactionType.EXPENSE);
        assertThat(result.confidence()).isEqualTo(QuickInputParser.Confidence.HIGH);
    }

    @Test
    void noteThenAmount() {
        var result = parseOne("tushlik 25000");
        assertThat(result.amount()).isEqualByComparingTo("25000.00");
        assertThat(result.categoryId()).isEqualTo(CAFE);
    }

    @Test
    void thousandAbbreviationK() {
        var result = parseOne("taksi 15k");
        assertThat(result.amount()).isEqualByComparingTo("15000.00");
        assertThat(result.categoryId()).isEqualTo(TRANSPORT);
    }

    @Test
    void thousandAbbreviationMing() {
        var result = parseOne("taksi 15 ming");
        assertThat(result.amount()).isEqualByComparingTo("15000.00");
    }

    @Test
    void millionAbbreviationMlnWithNoCategoryMatch() {
        var result = parseOne("kredit 2 mln");
        assertThat(result.amount()).isEqualByComparingTo("2000000.00");
        assertThat(result.categoryId()).isNull();
        assertThat(result.confidence()).isEqualTo(QuickInputParser.Confidence.LOW);
        assertThat(result.type()).isEqualTo(TransactionType.EXPENSE);
    }

    @Test
    void decimalMillion() {
        var result = parseOne("1.5 mln");
        assertThat(result.amount()).isEqualByComparingTo("1500000.00");
    }

    @Test
    void dotAsThousandSeparatorWithoutSuffix() {
        var result = parseOne("25.000");
        assertThat(result.amount()).isEqualByComparingTo("25000.00");
        assertThat(result.categoryId()).isNull();
    }

    @Test
    void spaceAsThousandSeparator() {
        var result = parseOne("kofe 12 000 naqd");
        assertThat(result.amount()).isEqualByComparingTo("12000.00");
        assertThat(result.categoryId()).isEqualTo(CAFE);
        assertThat(result.accountHint()).isEqualTo(QuickInputParser.AccountHint.CASH);
    }

    @Test
    void cardAccountHint() {
        var result = parseOne("taksi 20000 karta");
        assertThat(result.accountHint()).isEqualTo(QuickInputParser.AccountHint.BANK_CARD);
    }

    @Test
    void plusPrefixForcesIncome() {
        var result = parseOne("+8500000 oylik");
        assertThat(result.amount()).isEqualByComparingTo("8500000.00");
        assertThat(result.type()).isEqualTo(TransactionType.INCOME);
        assertThat(result.categoryId()).isEqualTo(SALARY);
    }

    @Test
    void incomeKeywordWithoutPlusPrefix() {
        var result = parseOne("maosh 3000000");
        assertThat(result.type()).isEqualTo(TransactionType.INCOME);
        assertThat(result.categoryId()).isEqualTo(SALARY);
    }

    @Test
    void incomeCategoryWithoutIncomeKeywordStillTypesAsIncome() {
        var result = parseOne("frilans 1200000");
        assertThat(result.type()).isEqualTo(TransactionType.INCOME);
        assertThat(result.categoryId()).isEqualTo(FREELANCE);
    }

    @Test
    void cyrillicKeywordMatch() {
        var result = parseOne("такси 15000");
        assertThat(result.amount()).isEqualByComparingTo("15000.00");
        assertThat(result.categoryId()).isEqualTo(TRANSPORT);
    }

    @Test
    void cyrillicIncomeKeyword() {
        var result = parseOne("зарплата 3000000");
        assertThat(result.type()).isEqualTo(TransactionType.INCOME);
        assertThat(result.categoryId()).isEqualTo(SALARY);
    }

    @Test
    void cyrillicMillionAbbreviation() {
        var result = parseOne("кредит 2 млн");
        assertThat(result.amount()).isEqualByComparingTo("2000000.00");
        assertThat(result.categoryId()).isNull();
    }

    @Test
    void unknownWordYieldsLowConfidence() {
        var result = parseOne("25000 asdqwe");
        assertThat(result.categoryId()).isNull();
        assertThat(result.confidence()).isEqualTo(QuickInputParser.Confidence.LOW);
    }

    @Test
    void textWithoutAmountIsNotATransaction() {
        Optional<QuickInputParser.ParsedLine> result = parser.parseLine("salom qalaysiz", CATEGORIES, Map.of());
        assertThat(result).isEmpty();
    }

    @Test
    void multipleLinesProduceOneTransactionEach() {
        List<QuickInputParser.ParsedLine> results =
                parser.parse("25000 tushlik\ntaksi 15k", CATEGORIES, Map.of());
        assertThat(results).hasSize(2);
        assertThat(results.get(0).categoryId()).isEqualTo(CAFE);
        assertThat(results.get(1).categoryId()).isEqualTo(TRANSPORT);
        assertThat(results.get(1).amount()).isEqualByComparingTo("15000.00");
    }

    @Test
    void linesWithoutAnAmountAreSkippedInMultiLineInput() {
        List<QuickInputParser.ParsedLine> results =
                parser.parse("salom\n25000 tushlik\nrahmat", CATEGORIES, Map.of());
        assertThat(results).hasSize(1);
        assertThat(results.get(0).categoryId()).isEqualTo(CAFE);
    }

    @Test
    void parsingMemoryTakesPriorityOverKeywords() {
        UUID learned = UUID.randomUUID();
        Map<String, UUID> memory = Map.of(QuickInputParser.normalizePhrase("sut"), learned);
        var result = parser.parseLine("sut 12000", CATEGORIES, memory).orElseThrow();
        assertThat(result.categoryId()).isEqualTo(learned);
        // Unknown to CATEGORIES, so type can't be inferred from it -- falls back to EXPENSE.
        assertThat(result.type()).isEqualTo(TransactionType.EXPENSE);
    }

    @Test
    void apostropheVariantsAreNormalizedInKeywordMatching() {
        UUID education = UUID.randomUUID();
        List<QuickInputParser.CategoryKeyword> categories = List.of(
                new QuickInputParser.CategoryKeyword(education, CategoryType.EXPENSE, List.of("o'qish")));
        var result = parser.parseLine("o‘qish 500000", categories, Map.of()).orElseThrow();
        assertThat(result.categoryId()).isEqualTo(education);
    }

    @Test
    void noDateHintDefaultsToToday() {
        var result = parseOne("25000 tushlik");
        assertThat(result.daysAgo()).isZero();
    }

    @Test
    void yesterdayKeywordUz() {
        var result = parseOne("kecha 25000 tushlik");
        assertThat(result.daysAgo()).isEqualTo(1);
        assertThat(result.amount()).isEqualByComparingTo("25000.00");
        assertThat(result.categoryId()).isEqualTo(CAFE);
        assertThat(result.rawLine()).isEqualTo("kecha 25000 tushlik");
    }

    @Test
    void yesterdayKeywordRu() {
        var result = parseOne("вчера 25000 обед");
        assertThat(result.daysAgo()).isEqualTo(1);
        assertThat(result.categoryId()).isEqualTo(CAFE);
    }

    @Test
    void todayKeywordIsExplicitZero() {
        var result = parseOne("bugun 25000 tushlik");
        assertThat(result.daysAgo()).isZero();
        assertThat(result.categoryId()).isEqualTo(CAFE);
    }

    @Test
    void daysAgoPhraseUz() {
        var result = parseOne("3 kun oldin taksi 15k");
        assertThat(result.daysAgo()).isEqualTo(3);
        assertThat(result.amount()).isEqualByComparingTo("15000.00");
        assertThat(result.categoryId()).isEqualTo(TRANSPORT);
    }

    @Test
    void daysAgoPhraseUzAvvalVariant() {
        var result = parseOne("taksi 15k 5 kun avval");
        assertThat(result.daysAgo()).isEqualTo(5);
        assertThat(result.amount()).isEqualByComparingTo("15000.00");
    }

    @Test
    void daysAgoPhraseRu() {
        var result = parseOne("2 дня назад такси 15000");
        assertThat(result.daysAgo()).isEqualTo(2);
        assertThat(result.categoryId()).isEqualTo(TRANSPORT);
    }

    @Test
    void dateHintAtEndOfLineDoesNotConfuseAmountParsing() {
        var result = parseOne("tushlik 25000 kecha");
        assertThat(result.daysAgo()).isEqualTo(1);
        assertThat(result.amount()).isEqualByComparingTo("25000.00");
        assertThat(result.categoryId()).isEqualTo(CAFE);
    }

}
