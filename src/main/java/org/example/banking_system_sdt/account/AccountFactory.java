package org.example.banking_system_sdt.account;

import org.example.banking_system_sdt.exception.BadRequestException;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AccountFactory {

    private final AtomicLong sequence = new AtomicLong(1);

    public Account openAccount(Long ownerId, BigDecimal initialDeposit) {
        BigDecimal deposit = initialDeposit != null ? initialDeposit : BigDecimal.ZERO;
        if (deposit.compareTo(minimumInitialDeposit()) < 0) {
            throw new BadRequestException("Initial deposit " + deposit + " EUR is below the minimum required " + minimumInitialDeposit() + " EUR");
        }

        String accountNumber = nextAccountNumber();
        Account account = createAccount(ownerId, accountNumber);

        if (deposit.compareTo(BigDecimal.ZERO) > 0) {
            account.credit(deposit);
        }

        return account;
    }

    public abstract AccountType supportedType();

    protected abstract Account createAccount(Long ownerId, String accountNumber);

    protected abstract String accountPrefix();

    protected abstract BigDecimal minimumInitialDeposit();

    protected String nextAccountNumber() {
        return String.format("%s%06d", accountPrefix(), sequence.getAndIncrement());
    }
}
