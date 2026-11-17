package org.example.banking_system_sdt.account;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class FixedFeeStrategy implements FeeStrategy {

    private final BigDecimal fee;

    public FixedFeeStrategy(BigDecimal fee) {
        if (fee == null || fee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Fee must be non-negative");
        }
        this.fee = fee.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateFee(BigDecimal amount) {
        return fee;
    }

    public BigDecimal getFee() {
        return fee;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FixedFeeStrategy that = (FixedFeeStrategy) o;
        return Objects.equals(fee, that.fee);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fee);
    }
}
