package org.example.banking_system_sdt.account.dto;

import org.example.banking_system_sdt.account.Account;
import org.example.banking_system_sdt.account.AccountStatus;
import org.example.banking_system_sdt.account.AccountType;
import org.example.banking_system_sdt.account.CheckingAccount;
import org.example.banking_system_sdt.account.SavingsAccount;

import java.math.BigDecimal;
import java.util.Set;

public record AccountResponse(
        Long id,
        String accountNumber,
        Long ownerId,
        Set<Long> authorizedCustomerIds,
        BigDecimal balance,
        String currency,
        AccountStatus status,
        AccountType type,
        BigDecimal availableToSpend,
        BigDecimal dailyTransferLimit,
        String feeStrategy,
        BigDecimal overdraftLimit,
        BigDecimal interestRate
) {
    public static AccountResponse from(Account account) {
        BigDecimal overdraft = null;
        BigDecimal interest = null;
        if (account instanceof CheckingAccount checking) {
            overdraft = checking.getOverdraftLimit();
        } else if (account instanceof SavingsAccount savings) {
            interest = savings.getInterestRate();
        }

        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getOwnerId(),
                account.getAuthorizedCustomerIds(),
                account.getBalance(),
                account.getCurrency(),
                account.getStatus(),
                account.getType(),
                account.availableToSpend(),
                account.getDailyTransferLimit(),
                account.getFeeStrategy() != null ? account.getFeeStrategy().getClass().getSimpleName() : null,
                overdraft,
                interest
        );
    }
}
