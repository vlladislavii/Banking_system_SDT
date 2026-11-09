package org.example.banking_system_sdt.account;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NoFeeStrategy implements FeeStrategy {

    private static final BigDecimal ZERO_FEE = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Override
    public BigDecimal calculateFee(BigDecimal amount) {
        return ZERO_FEE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
