package org.example.banking_system_sdt.account;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class PercentageFeeStrategy implements FeeStrategy {

    private final BigDecimal rate;

    public PercentageFeeStrategy(BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Rate must be non-negative");
        }
        this.rate = rate;
    }

    @Override
    public BigDecimal calculateFee(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.multiply(rate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getRate() {
        return rate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PercentageFeeStrategy that = (PercentageFeeStrategy) o;
        return Objects.equals(rate, that.rate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rate);
    }
}
