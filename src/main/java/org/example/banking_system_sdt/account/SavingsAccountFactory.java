package org.example.banking_system_sdt.account;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class SavingsAccountFactory extends AccountFactory {

    private static final BigDecimal DEFAULT_INTEREST_RATE = new BigDecimal("2.50");
    private static final BigDecimal DEFAULT_DAILY_TRANSFER_LIMIT = new BigDecimal("1000.00");
    private static final BigDecimal DEFAULT_RATE = new BigDecimal("0.5");
    private static final BigDecimal MIN_INITIAL_DEPOSIT = new BigDecimal("100.00");

    @Override
    public AccountType supportedType() {
        return AccountType.SAVINGS;
    }

    @Override
    protected Account createAccount(Long ownerId, String accountNumber) {
        FeeStrategy feeStrategy = new PercentageFeeStrategy(DEFAULT_RATE);
        return new SavingsAccount(
                ownerId,
                accountNumber,
                DEFAULT_DAILY_TRANSFER_LIMIT,
                feeStrategy,
                AccountStatus.ACTIVE,
                DEFAULT_INTEREST_RATE
        );
    }

    @Override
    protected String accountPrefix() {
        return "SAV-";
    }

    @Override
    protected BigDecimal minimumInitialDeposit() {
        return MIN_INITIAL_DEPOSIT;
    }
}
