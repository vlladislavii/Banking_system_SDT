package org.example.banking_system_sdt.transaction;

import java.math.BigDecimal;
import java.util.Set;

public record AccountSnapshot(
        Long id,
        Long ownerId,
        Set<Long> authorizedCustomerIds,
        boolean canSend,
        boolean canReceive,
        BigDecimal availableToSpend,
        BigDecimal dailyTransferLimit
) {
    public boolean hasAccess(Long userId) {
        if (userId == null) {
            return false;
        }
        return (ownerId != null && ownerId.equals(userId))
                || (authorizedCustomerIds != null && authorizedCustomerIds.contains(userId));
    }
}
