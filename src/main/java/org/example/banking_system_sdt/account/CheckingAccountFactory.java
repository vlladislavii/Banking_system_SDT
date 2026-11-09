package org.example.banking_system_sdt.account;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class CheckingAccountFactory extends AccountFactory {

    private static final BigDecimal DEFAULT_OVERDRAFT_LIMIT = new BigDecimal("500.00");
    private static final BigDecimal DEFAULT_DAILY_TRANSFER_LIMIT = new BigDecimal("5000.00");
    private static final BigDecimal DEFAULT_FEE = new BigDecimal("1.00");
    private static final BigDecimal MIN_INITIAL_DEPOSIT = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Override
    public AccountType supportedType() {
        return AccountType.CHECKING;
    }

    @Override
    protected Account createAccount(Long ownerId, String accountNumber) {
        FeeStrategy feeStrategy = new FixedFeeStrategy(DEFAULT_FEE);
        return new CheckingAccount(
                ownerId,
                accountNumber,
                DEFAULT_DAILY_TRANSFER_LIMIT,
                feeStrategy,
                AccountStatus.ACTIVE,
                DEFAULT_OVERDRAFT_LIMIT
        );
    }

    @Override
    protected String accountPrefix() {
        return "CHK-";
    }

    @Override
    protected BigDecimal minimumInitialDeposit() {
        return MIN_INITIAL_DEPOSIT;
    }
}
