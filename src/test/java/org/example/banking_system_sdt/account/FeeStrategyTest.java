package org.example.banking_system_sdt.account;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class FeeStrategyTest {

    private final FeeStrategyConverter converter = new FeeStrategyConverter();

    @Test
    void fixedFeeStrategy_returnsConfiguredFee() {
        FeeStrategy strategy = new FixedFeeStrategy(new BigDecimal("1.50"));

        assertEquals(new BigDecimal("1.50"), strategy.calculateFee(new BigDecimal("10.00")));
        assertEquals(new BigDecimal("1.50"), strategy.calculateFee(new BigDecimal("500.00")));
        assertEquals(new BigDecimal("1.50"), strategy.calculateFee(new BigDecimal("10000.00")));
    }

    @Test
    void fixedFeeStrategy_rejectsNegativeFee() {
        assertThrows(IllegalArgumentException.class, () -> new FixedFeeStrategy(new BigDecimal("-1.00")));
    }

    @Test
    void percentageFeeStrategy_calculatesPercentageCorrectly() {
        FeeStrategy strategy = new PercentageFeeStrategy(new BigDecimal("0.5"));

        // 200 * 0.5% = 1.00
        assertEquals(new BigDecimal("1.00"), strategy.calculateFee(new BigDecimal("200.00")));
        // 1000 * 0.5% = 5.00
        assertEquals(new BigDecimal("5.00"), strategy.calculateFee(new BigDecimal("1000.00")));
        // 50 * 0.5% = 0.25
        assertEquals(new BigDecimal("0.25"), strategy.calculateFee(new BigDecimal("50.00")));
    }

    @Test
    void percentageFeeStrategy_zeroAmountReturnsZero() {
        FeeStrategy strategy = new PercentageFeeStrategy(new BigDecimal("1.0"));
        assertEquals(new BigDecimal("0.00"), strategy.calculateFee(BigDecimal.ZERO));
    }

    @Test
    void percentageFeeStrategy_rejectsNegativeRate() {
        assertThrows(IllegalArgumentException.class, () -> new PercentageFeeStrategy(new BigDecimal("-0.5")));
    }

    @Test
    void noFeeStrategy_alwaysReturnsZero() {
        FeeStrategy strategy = new NoFeeStrategy();

        assertEquals(new BigDecimal("0.00"), strategy.calculateFee(new BigDecimal("100.00")));
        assertEquals(new BigDecimal("0.00"), strategy.calculateFee(new BigDecimal("5000.00")));
    }

    @Test
    void feeStrategyConverter_convertsFixedFeeStrategy() {
        FixedFeeStrategy original = new FixedFeeStrategy(new BigDecimal("2.50"));
        String dbValue = converter.convertToDatabaseColumn(original);
        assertEquals("FIXED:2.50", dbValue);

        FeeStrategy restored = converter.convertToEntityAttribute(dbValue);
        assertInstanceOf(FixedFeeStrategy.class, restored);
        assertEquals(original, restored);
    }

    @Test
    void feeStrategyConverter_convertsPercentageFeeStrategy() {
        PercentageFeeStrategy original = new PercentageFeeStrategy(new BigDecimal("0.75"));
        String dbValue = converter.convertToDatabaseColumn(original);
        assertEquals("PERCENTAGE:0.75", dbValue);

        FeeStrategy restored = converter.convertToEntityAttribute(dbValue);
        assertInstanceOf(PercentageFeeStrategy.class, restored);
        assertEquals(original, restored);
    }

    @Test
    void feeStrategyConverter_convertsNoFeeStrategy() {
        NoFeeStrategy original = new NoFeeStrategy();
        String dbValue = converter.convertToDatabaseColumn(original);
        assertEquals("NONE", dbValue);

        FeeStrategy restored = converter.convertToEntityAttribute(dbValue);
        assertInstanceOf(NoFeeStrategy.class, restored);
        assertEquals(original, restored);
    }

    @Test
    void feeStrategyConverter_handlesNull() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToEntityAttribute("   "));
    }

    @Test
    void feeStrategyConverter_rejectsInvalidDatabaseValue() {
        assertThrows(IllegalArgumentException.class, () -> converter.convertToEntityAttribute("INVALID:10"));
    }
}
