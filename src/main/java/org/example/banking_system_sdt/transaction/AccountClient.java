package org.example.banking_system_sdt.transaction;

import java.math.BigDecimal;
import java.util.Optional;

public interface AccountClient {
    Optional<AccountSnapshot> getAccount(Long accountId);
    BigDecimal quoteFee(Long accountId, BigDecimal amount);
    void applyTransfer(Long sourceId, Long targetId, BigDecimal amount, BigDecimal fee);
    boolean isAdmin(Long userId);
}
