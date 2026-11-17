package org.example.banking_system_sdt.transaction;

import java.math.BigDecimal;

public record TransferRequest(
        Long callerId,
        Long sourceAccountId,
        Long targetAccountId,
        BigDecimal amount
) {}
