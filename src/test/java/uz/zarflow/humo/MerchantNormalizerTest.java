package uz.zarflow.humo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantNormalizerTest {

    @Test
    void stripsCodeWordAndCutsFromArrow() {
        assertThat(MerchantNormalizer.normalize("MCHJ ESTELINE>TASHKE")).isEqualTo("ESTELINE");
    }

    @Test
    void stripsCodeWordAndTrailingUz() {
        assertThat(MerchantNormalizer.normalize("UZUM CHECKOUT OOO UZ")).isEqualTo("UZUM CHECKOUT");
    }

    @Test
    void cutsFromArrowWithNoTrailingContent() {
        assertThat(MerchantNormalizer.normalize("PAYME OPLATA CREDIT>")).isEqualTo("PAYME OPLATA CREDIT");
    }

    @Test
    void flagsP2pMerchants() {
        assertThat(MerchantNormalizer.isP2p(MerchantNormalizer.normalize("TBC HUMO P2P>TASHKEN"))).isTrue();
        assertThat(MerchantNormalizer.isP2p(MerchantNormalizer.normalize("TBC P2P S UZCARD NA"))).isTrue();
        assertThat(MerchantNormalizer.isP2p(MerchantNormalizer.normalize("ZOOMRAD P2P HUMO2VIS"))).isTrue();
        assertThat(MerchantNormalizer.isP2p(MerchantNormalizer.normalize("UZUM CHECKOUT OOO UZ"))).isFalse();
    }

    @Test
    void flagsLoanMerchants() {
        assertThat(MerchantNormalizer.isLoan(MerchantNormalizer.normalize("PAYME OPLATA CREDIT>"))).isTrue();
        assertThat(MerchantNormalizer.isLoan(MerchantNormalizer.normalize("UZUM CHECKOUT OOO UZ"))).isFalse();
    }

    @Test
    void resolvesCategoryHints() {
        assertThat(MerchantNormalizer.categoryHintCode(MerchantNormalizer.normalize("UZUM CHECKOUT OOO UZ")))
                .contains("shopping");
        assertThat(MerchantNormalizer.categoryHintCode(MerchantNormalizer.normalize("OOO WB TAXI OPLATA>T")))
                .contains("transport");
        assertThat(MerchantNormalizer.categoryHintCode(MerchantNormalizer.normalize("TRANSPORT TOLOV>TOS")))
                .contains("transport");
        assertThat(MerchantNormalizer.categoryHintCode("UNKNOWN MERCHANT")).isEmpty();
    }

}
