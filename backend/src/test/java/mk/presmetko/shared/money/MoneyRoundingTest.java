package mk.presmetko.shared.money;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyRoundingTest {

    @Test
    void linesHaveFourDecimalsHalfUp() {
        assertThat(MoneyRounding.line(new BigDecimal("95.238095"))).isEqualByComparingTo("95.2381");
        assertThat(MoneyRounding.line(new BigDecimal("0.00005"))).isEqualByComparingTo("0.0001");
        assertThat(MoneyRounding.line(new BigDecimal("1")).scale()).isEqualTo(4);
    }

    @Test
    void totalsHaveTwoDecimalsHalfUp() {
        assertThat(MoneyRounding.total(new BigDecimal("0.125"))).isEqualByComparingTo("0.13");
        assertThat(MoneyRounding.total(new BigDecimal("4.7619"))).isEqualByComparingTo("4.76");
    }

    @Test
    void grossRoundedIsInteger() {
        assertThat(MoneyRounding.grossRounded(new BigDecimal("100.50"))).isEqualByComparingTo("101");
        assertThat(MoneyRounding.grossRounded(new BigDecimal("100.49"))).isEqualByComparingTo("100");
        assertThat(MoneyRounding.grossRounded(new BigDecimal("100.50")).scale()).isZero();
    }
}
