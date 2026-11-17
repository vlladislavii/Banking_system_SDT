package org.example.banking_system_sdt.transaction.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferApiRequest(
        @NotNull(message = "Source account ID is required") Long sourceAccountId,
        @NotNull(message = "Target account ID is required") Long targetAccountId,
        @NotNull(message = "Amount is required") @Positive(message = "Amount must be positive") BigDecimal amount
) {}
