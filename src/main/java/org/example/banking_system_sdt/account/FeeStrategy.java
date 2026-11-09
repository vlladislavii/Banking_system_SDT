package org.example.banking_system_sdt.account;

import java.math.BigDecimal;

public interface FeeStrategy {
    BigDecimal calculateFee(BigDecimal amount);
}
