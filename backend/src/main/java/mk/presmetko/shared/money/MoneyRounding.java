package mk.presmetko.shared.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Правило за заокружување R2026-1 (техничка спец. 2.0, дел 10): HALF_UP; ставки на 4 децимали, збирови на 2,
 * {@code docGrossAmountR} цел број. Пари се секогаш {@link BigDecimal} (во базата {@code NUMERIC(19,4)}), никогаш
 * {@code double}.
 */
public final class MoneyRounding {

    public static final String RULE = "R2026-1";
    public static final RoundingMode MODE = RoundingMode.HALF_UP;
    public static final int LINE_SCALE = 4;
    public static final int TOTAL_SCALE = 2;

    private MoneyRounding() {}

    /** round4: износи на ниво на ставка. */
    public static BigDecimal line(BigDecimal amount) {
        return amount.setScale(LINE_SCALE, MODE);
    }

    /** round2: збирови на документ. */
    public static BigDecimal total(BigDecimal amount) {
        return amount.setScale(TOTAL_SCALE, MODE);
    }

    /** round0: {@code docGrossAmountR}. */
    public static BigDecimal grossRounded(BigDecimal amount) {
        return amount.setScale(0, MODE);
    }
}
