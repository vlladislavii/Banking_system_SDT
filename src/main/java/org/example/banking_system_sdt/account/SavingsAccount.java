package org.example.banking_system_sdt.account;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@DiscriminatorValue("SAVINGS")
public class SavingsAccount extends Account {

    @Column
    private BigDecimal interestRate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    public SavingsAccount() {
    }

    public SavingsAccount(Long ownerId, String accountNumber, BigDecimal dailyTransferLimit,
                          FeeStrategy feeStrategy, AccountStatus status, BigDecimal interestRate) {
        super(ownerId, accountNumber, dailyTransferLimit, feeStrategy, status);
        this.interestRate = interestRate != null ? interestRate.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal availableToSpend() {
        return getBalance();
    }

    @Override
    public AccountType getType() {
        return AccountType.SAVINGS;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate != null ? interestRate.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
