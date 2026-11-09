package org.example.banking_system_sdt.account;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@DiscriminatorValue("CHECKING")
public class CheckingAccount extends Account {

    @Column
    private BigDecimal overdraftLimit = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    public CheckingAccount() {
    }

    public CheckingAccount(Long ownerId, String accountNumber, BigDecimal dailyTransferLimit,
                           FeeStrategy feeStrategy, AccountStatus status, BigDecimal overdraftLimit) {
        super(ownerId, accountNumber, dailyTransferLimit, feeStrategy, status);
        this.overdraftLimit = overdraftLimit != null ? overdraftLimit.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal availableToSpend() {
        return getBalance().add(overdraftLimit).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public AccountType getType() {
        return AccountType.CHECKING;
    }

    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }

    public void setOverdraftLimit(BigDecimal overdraftLimit) {
        this.overdraftLimit = overdraftLimit != null ? overdraftLimit.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
