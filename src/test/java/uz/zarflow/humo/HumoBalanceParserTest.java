package uz.zarflow.humo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HumoBalanceParserTest {

    @Test
    void parsesAllSevenCardsFromTheFixture() {
        List<HumoBalanceParser.HumoCardAmount> cards = HumoBalanceParser.parse(readFixture());

        assertThat(cards).hasSize(7);
        assertThat(cards).extracting(HumoBalanceParser.HumoCardAmount::cardLast4)
                .containsExactly("1111", "2222", "3333", "4444", "5555", "6666", "7777");
        assertThat(cards.get(0).balance()).isEqualByComparingTo("0.00");
        assertThat(cards.get(1).balance()).isEqualByComparingTo("623.47");
        assertThat(cards.get(2).balance()).isEqualByComparingTo("109151.58");
        assertThat(cards.get(3).balance()).isEqualByComparingTo("30810.64");
        assertThat(cards.get(4).balance()).isEqualByComparingTo("0.00");
        assertThat(cards.get(5).balance()).isEqualByComparingTo("1250000.00");
        assertThat(cards.get(6).balance()).isEqualByComparingTo("0.00");
    }

    @Test
    void zeroBalanceCardsAreStillParsedAsZero() {
        List<HumoBalanceParser.HumoCardAmount> cards = HumoBalanceParser.parse(readFixture());
        long zeroCount = cards.stream().filter(c -> c.balance().signum() == 0).count();

        assertThat(zeroCount).isEqualTo(3);
    }

    @Test
    void plainDecimalAndApostropheGroupedAmountsParseSideBySide() {
        String text = """
                🔹 HUMOCARD *0001
                💵 0.00 UZS
                🔹 HUMOCARD *0002
                💵 623.47 UZS
                🔹 HUMOCARD *0003
                💵 109'151.58 UZS
                """;

        List<HumoBalanceParser.HumoCardAmount> cards = HumoBalanceParser.parse(text);

        assertThat(cards).extracting(HumoBalanceParser.HumoCardAmount::balance)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("0.00"), new BigDecimal("623.47"), new BigDecimal("109151.58"));
    }

    @Test
    void curlyApostropheIsTreatedAsThousandsSeparatorToo() {
        String text = "🔹 HUMOCARD *9999\n💵 1’250’000.00 UZS\n";

        List<HumoBalanceParser.HumoCardAmount> cards = HumoBalanceParser.parse(text);

        assertThat(cards).hasSize(1);
        assertThat(cards.get(0).balance()).isEqualByComparingTo("1250000.00");
    }

    @Test
    void textWithNoCardBlockIsNotRecognizedAsAHumoMessage() {
        assertThat(HumoBalanceParser.parse("25000 tushlik")).isEmpty();
        assertThat(HumoBalanceParser.parse(null)).isEmpty();
        assertThat(HumoBalanceParser.parse("")).isEmpty();
    }

    private String readFixture() {
        try (InputStream in = getClass().getResourceAsStream("/humo/balance.txt")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

}