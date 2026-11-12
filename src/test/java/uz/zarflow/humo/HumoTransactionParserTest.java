package uz.zarflow.humo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import uz.zarflow.humo.HumoTransactionParser.HumoTransactionBlock;
import uz.zarflow.humo.HumoTransactionParser.ParsedBatch;
import uz.zarflow.transaction.TransactionType;

import static org.assertj.core.api.Assertions.assertThat;

class HumoTransactionParserTest {

    private static final ZoneId TASHKENT = ZoneId.of("Asia/Tashkent");

    @Test
    void parsesAllForwardedBlocks() {
        ParsedBatch batch = HumoTransactionParser.parse(fixture("transactions_forwarded.txt"));

        assertThat(batch.skippedNonUzs()).isZero();
        assertThat(batch.blocks()).hasSize(5);

        HumoTransactionBlock first = batch.blocks().get(0);
        assertThat(first.type()).isEqualTo(TransactionType.EXPENSE);
        assertThat(first.amount()).isEqualByComparingTo(new BigDecimal("54300.00"));
        assertThat(first.merchantRaw()).isEqualTo("UZUM CHECKOUT OOO UZ");
        assertThat(first.merchantKey()).isEqualTo("UZUM CHECKOUT");
        assertThat(first.cardLast4()).isEqualTo("1111");
        assertThat(first.transactionDate()).isEqualTo(
                ZonedDateTime.of(2026, 11, 11, 11, 59, 0, 0, TASHKENT).toInstant());

        HumoTransactionBlock topUp = batch.blocks().get(2);
        assertThat(topUp.type()).isEqualTo(TransactionType.INCOME);
        assertThat(topUp.amount()).isEqualByComparingTo(new BigDecimal("300000.00"));

        HumoTransactionBlock secondCard = batch.blocks().get(4);
        assertThat(secondCard.cardLast4()).isEqualTo("2222");
        assertThat(secondCard.merchantKey()).isEqualTo("ZOOMRAD P2P HUMO2VIS");
    }

    @Test
    void parsesPastedBlocksAndKeepsExactDuplicates() {
        ParsedBatch batch = HumoTransactionParser.parse(fixture("transactions_pasted.txt"));

        assertThat(batch.blocks()).hasSize(5);
        assertThat(batch.blocks().get(1)).isEqualTo(batch.blocks().get(3));
        assertThat(batch.blocks().get(2)).isEqualTo(batch.blocks().get(4));
    }

    @Test
    void flagsP2pBlocks() {
        List<HumoTransactionBlock> blocks = HumoTransactionParser.parse(fixture("transactions_forwarded.txt")).blocks();

        assertThat(MerchantNormalizer.isP2p(blocks.get(1).merchantKey())).isTrue(); // TBC HUMO P2P
        assertThat(MerchantNormalizer.isP2p(blocks.get(2).merchantKey())).isTrue(); // TBC P2P S UZCARD
        assertThat(MerchantNormalizer.isP2p(blocks.get(4).merchantKey())).isTrue(); // ZOOMRAD P2P HUMO2VIS
        assertThat(MerchantNormalizer.isP2p(blocks.get(0).merchantKey())).isFalse();
    }

    @Test
    void flagsLoanBlock() {
        HumoTransactionBlock payme = HumoTransactionParser.parse(fixture("transactions_pasted.txt")).blocks().get(0);

        assertThat(payme.merchantKey()).isEqualTo("PAYME OPLATA CREDIT");
        assertThat(MerchantNormalizer.isLoan(payme.merchantKey())).isTrue();
    }

    @Test
    void returnsEmptyForNonHumoText() {
        ParsedBatch batch = HumoTransactionParser.parse("Taxi 25000 kecha");

        assertThat(batch.blocks()).isEmpty();
        assertThat(batch.skippedNonUzs()).isZero();
    }

    @Test
    void parsesARealSingleTransactionMessageCorrectly() {
        ParsedBatch batch = HumoTransactionParser.parse(fixture("transaction_real_single.txt"));

        assertThat(batch.skippedNonUzs()).isZero();
        assertThat(batch.blocks()).hasSize(1);
        HumoTransactionBlock block = batch.blocks().get(0);
        assertThat(block.type()).isEqualTo(TransactionType.EXPENSE);
        assertThat(block.amount()).isEqualByComparingTo(new BigDecimal("47000.00"));
        assertThat(block.merchantRaw()).isEqualTo("ZIYODA BARAKA BIZNES");
        assertThat(block.cardLast4()).isEqualTo("8643");
    }

    @Test
    void skipsNonUzsAmounts() {
        String block = """
                💸 Оплата
                ➖ 10,00 USD
                📍 FOREIGN MERCHANT
                💳 HUMOCARD *9999
                🕓 10:00 01.01.2026
                """;

        ParsedBatch batch = HumoTransactionParser.parse(block);

        assertThat(batch.blocks()).isEmpty();
        assertThat(batch.skippedNonUzs()).isEqualTo(1);
    }

    private String fixture(String name) {
        try {
            return Files.readString(Path.of("src/test/resources/humo", name), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

}
