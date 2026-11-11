package uz.zarflow.humo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import uz.zarflow.humo.HumoTransactionParser.HumoTransactionBlock;

import static org.assertj.core.api.Assertions.assertThat;

class HumoImportFingerprintTest {

    @Test
    void pastedBatchHasExactlyThreeDistinctFingerprints() {
        List<HumoTransactionBlock> blocks = HumoTransactionParser.parse(fixture("transactions_pasted.txt")).blocks();

        Set<String> fingerprints = blocks.stream().map(HumoImportFingerprint::of).collect(Collectors.toSet());

        assertThat(blocks).hasSize(5);
        assertThat(fingerprints).hasSize(3);
    }

    @Test
    void reimportingEveryForwardedBlockProducesTheSameFingerprintsAgain() {
        List<HumoTransactionBlock> first = HumoTransactionParser.parse(fixture("transactions_forwarded.txt")).blocks();
        List<HumoTransactionBlock> second = HumoTransactionParser.parse(fixture("transactions_forwarded.txt")).blocks();

        List<String> firstFingerprints = first.stream().map(HumoImportFingerprint::of).toList();
        List<String> secondFingerprints = second.stream().map(HumoImportFingerprint::of).toList();

        assertThat(secondFingerprints).isEqualTo(firstFingerprints);
        assertThat(Set.copyOf(firstFingerprints)).hasSize(first.size());
    }

    @Test
    void differentAmountProducesDifferentFingerprint() {
        List<HumoTransactionBlock> blocks = HumoTransactionParser.parse(fixture("transactions_forwarded.txt")).blocks();

        String a = HumoImportFingerprint.of(blocks.get(0));
        String b = HumoImportFingerprint.of(blocks.get(1));

        assertThat(a).isNotEqualTo(b);
    }

    private String fixture(String name) {
        try {
            return Files.readString(Path.of("src/test/resources/humo", name), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

}
