package uz.zarflow.insight;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import uz.zarflow.insight.InsightRuleEngine.Candidate;
import uz.zarflow.insight.InsightRuleEngine.CategoryWeekChange;
import uz.zarflow.insight.InsightRuleEngine.Input;

import static org.assertj.core.api.Assertions.assertThat;

class InsightRuleEngineTest {

    private static final UUID CATEGORY_ID = UUID.randomUUID();

    @Test
    void categoryUpWeekFiresAtExactlyThirtyPercentAndFiftyThousand() {
        // last week 100 000, this week 130 000: +30% and +30 000 -- amount is below the floor.
        List<Candidate> belowAmount = InsightRuleEngine.evaluate(
                categoryInput(new CategoryWeekChange(CATEGORY_ID, "Kafe", bd(130_000), bd(100_000))));
        assertThat(codesOf(belowAmount)).doesNotContain(InsightCode.CATEGORY_UP_WEEK);

        // last week 166 000, this week 216 000: +50 000 and ~30.1% -- both floors cleared.
        List<Candidate> fires = InsightRuleEngine.evaluate(
                categoryInput(new CategoryWeekChange(CATEGORY_ID, "Kafe", bd(216_000), bd(166_000))));
        assertThat(codesOf(fires)).contains(InsightCode.CATEGORY_UP_WEEK);
    }

    @Test
    void categoryUpWeekDoesNotFireJustUnderTheThirtyPercentFloor() {
        // last week 200 000, this week 258 000: +58 000 (over the amount floor) but only +29%.
        List<Candidate> result = InsightRuleEngine.evaluate(
                categoryInput(new CategoryWeekChange(CATEGORY_ID, "Kafe", bd(258_000), bd(200_000))));
        assertThat(codesOf(result)).doesNotContain(InsightCode.CATEGORY_UP_WEEK);
    }

    @Test
    void categoryUpWeekNeedsANonZeroLastWeekBaseline() {
        List<Candidate> result = InsightRuleEngine.evaluate(
                categoryInput(new CategoryWeekChange(CATEGORY_ID, "Kafe", bd(100_000), BigDecimal.ZERO)));
        assertThat(codesOf(result)).doesNotContain(InsightCode.CATEGORY_UP_WEEK);
    }

    @Test
    void budget80FiresAtTheThresholdWithEnoughDaysLeft() {
        Input input = new Input(
                List.of(), bd(1_000_000), bd(800_000), 5, null, false, null, null, null, null, 0);
        assertThat(codesOf(InsightRuleEngine.evaluate(input))).contains(InsightCode.BUDGET_80);
    }

    @Test
    void budget80DoesNotFireWithFewerThanFiveDaysLeft() {
        Input input = new Input(
                List.of(), bd(1_000_000), bd(900_000), 4, null, false, null, null, null, null, 0);
        assertThat(codesOf(InsightRuleEngine.evaluate(input))).doesNotContain(InsightCode.BUDGET_80);
    }

    @Test
    void budget80DoesNotFireJustUnderEightyPercent() {
        Input input = new Input(
                List.of(), bd(1_000_000), bd(799_999), 10, null, false, null, null, null, null, 0);
        assertThat(codesOf(InsightRuleEngine.evaluate(input))).doesNotContain(InsightCode.BUDGET_80);
    }

    @Test
    void forecastOverFiresWhenTheCalculatorAlreadySaysForecastIsPositive() {
        Input input = new Input(
                List.of(), null, null, 10, bd(150_000), true, null, null, null, null, 0);
        assertThat(codesOf(InsightRuleEngine.evaluate(input))).contains(InsightCode.FORECAST_OVER);
    }

    @Test
    void forecastOverDoesNotFireWhenNotPositive() {
        Input input = new Input(
                List.of(), null, null, 10, bd(-50_000), false, null, null, null, null, 0);
        assertThat(codesOf(InsightRuleEngine.evaluate(input))).doesNotContain(InsightCode.FORECAST_OVER);
    }

    @Test
    void debtShareFiresAtExactlyTwentyPercentOfIncome() {
        Input input = new Input(
                List.of(), null, null, 10, null, false, bd(200_000), bd(1_000_000), null, null, 0);
        assertThat(codesOf(InsightRuleEngine.evaluate(input))).contains(InsightCode.DEBT_SHARE);
    }

    @Test
    void debtShareDoesNotFireJustUnderTwentyPercent() {
        Input input = new Input(
                List.of(), null, null, 10, null, false, bd(199_999), bd(1_000_000), null, null, 0);
        assertThat(codesOf(InsightRuleEngine.evaluate(input))).doesNotContain(InsightCode.DEBT_SHARE);
    }

    @Test
    void discretionaryDownFiresOnAnyDecreaseVsSamePeriodLastMonth() {
        Input input = new Input(
                List.of(), null, null, 10, null, false, null, null, bd(300_000), bd(440_000), 0);
        List<Candidate> result = InsightRuleEngine.evaluate(input);
        assertThat(codesOf(result)).contains(InsightCode.DISCRETIONARY_DOWN);
        assertThat(result.get(0).payload().get("diffAmount")).isEqualTo("140000");
    }

    @Test
    void discretionaryDownDoesNotFireWhenSpendingIsFlatOrHigher() {
        Input input = new Input(
                List.of(), null, null, 10, null, false, null, null, bd(440_000), bd(440_000), 0);
        assertThat(codesOf(InsightRuleEngine.evaluate(input))).doesNotContain(InsightCode.DISCRETIONARY_DOWN);
    }

    @Test
    void noEntriesFiresAtExactlyThreeDaysButNotAtTwo() {
        Input twoDays = new Input(List.of(), null, null, 10, null, false, null, null, null, null, 2);
        assertThat(codesOf(InsightRuleEngine.evaluate(twoDays))).doesNotContain(InsightCode.NO_ENTRIES_3_DAYS);

        Input threeDays = new Input(List.of(), null, null, 10, null, false, null, null, null, null, 3);
        assertThat(codesOf(InsightRuleEngine.evaluate(threeDays))).contains(InsightCode.NO_ENTRIES_3_DAYS);
    }

    @Test
    void noDataAtAllProducesNoInsights() {
        Input empty = new Input(List.of(), null, null, 31, null, false, null, null, null, null, 0);
        assertThat(InsightRuleEngine.evaluate(empty)).isEmpty();
    }

    @Test
    void rankingPutsThePositiveInsightFirstThenActionableThenInformational() {
        // DISCRETIONARY_DOWN (positive), BUDGET_80 (actionable), and DEBT_SHARE (informational)
        // all fire at once -- the ranked order must be exactly that, regardless of evaluation order.
        Input input = new Input(
                List.of(), bd(1_000_000), bd(850_000), 10, null, false,
                bd(250_000), bd(1_000_000), bd(100_000), bd(300_000), 0);

        List<InsightCode> order = codesOf(InsightRuleEngine.evaluate(input));

        assertThat(order).containsExactly(InsightCode.DISCRETIONARY_DOWN, InsightCode.BUDGET_80, InsightCode.DEBT_SHARE);
    }

    @Test
    void multipleCategorySpikesRankByOverageAmountDescending() {
        UUID kafe = UUID.randomUUID();
        UUID transport = UUID.randomUUID();
        Input input = new Input(
                List.of(
                        new CategoryWeekChange(kafe, "Kafe", bd(216_000), bd(166_000)), // +50 000
                        new CategoryWeekChange(transport, "Transport", bd(400_000), bd(200_000))), // +200 000
                null, null, 10, null, false, null, null, null, null, 0);

        List<Candidate> ranked = InsightRuleEngine.evaluate(input);

        assertThat(ranked).hasSize(2);
        assertThat(ranked.get(0).payload().get("categoryName")).isEqualTo("Transport");
        assertThat(ranked.get(1).payload().get("categoryName")).isEqualTo("Kafe");
    }

    @Test
    void priorityIsAssignedAsASequentialRankStartingAtZero() {
        Input input = new Input(
                List.of(), bd(1_000_000), bd(850_000), 10, null, false,
                bd(250_000), bd(1_000_000), null, null, 0);

        List<Candidate> ranked = InsightRuleEngine.evaluate(input);

        assertThat(ranked).extracting(Candidate::priority).containsExactly(0, 1);
    }

    @Test
    void dedupeByPeriodKeyIsTheCallersJobNotTheEngines() {
        // The engine itself is stateless per call -- evaluating the identical input twice
        // yields the identical candidate list both times; period-key dedupe happens where
        // the service persists candidates against the (user, code, period_key) unique index.
        Input input = new Input(List.of(), null, null, 10, null, false, null, null, null, null, 3);
        assertThat(InsightRuleEngine.evaluate(input)).isEqualTo(InsightRuleEngine.evaluate(input));
    }

    private static List<InsightCode> codesOf(List<Candidate> candidates) {
        return candidates.stream().map(Candidate::code).toList();
    }

    private static BigDecimal bd(long value) {
        return BigDecimal.valueOf(value);
    }

    private static Input categoryInput(CategoryWeekChange change) {
        return new Input(List.of(change), null, null, 10, null, false, null, null, null, null, 0);
    }

}
