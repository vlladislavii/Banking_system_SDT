package org.example.banking_system_sdt.account.dto;

import jakarta.validation.constraints.NotNull;
import org.example.banking_system_sdt.account.AccountType;

import java.math.BigDecimal;

public record OpenAccountRequest(
        @NotNull(message = "Owner ID is required") Long ownerId,
        @NotNull(message = "Account type is required") AccountType type,
        BigDecimal initialDeposit
) {}
