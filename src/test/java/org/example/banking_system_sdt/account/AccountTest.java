package org.example.banking_system_sdt.account;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    void checkingAccount_availableToSpendIncludesOverdraft() {
        CheckingAccount checking = new CheckingAccount(
                10L, "CHK-000001", new BigDecimal("5000.00"),
                new FixedFeeStrategy(new BigDecimal("1.00")),
                AccountStatus.ACTIVE, new BigDecimal("500.00")
        );

        checking.credit(new BigDecimal("200.00"));
        assertEquals(new BigDecimal("200.00"), checking.getBalance());
        assertEquals(new BigDecimal("700.00"), checking.availableToSpend());

        checking.debit(new BigDecimal("600.00"));
        assertEquals(new BigDecimal("-400.00"), checking.getBalance());
        assertEquals(new BigDecimal("100.00"), checking.availableToSpend());
    }

    @Test
    void savingsAccount_availableToSpendEqualsBalance() {
        SavingsAccount savings = new SavingsAccount(
                10L, "SAV-000001", new BigDecimal("1000.00"),
                new PercentageFeeStrategy(new BigDecimal("0.5")),
                AccountStatus.ACTIVE, new BigDecimal("2.50")
        );

        savings.credit(new BigDecimal("300.00"));
        assertEquals(new BigDecimal("300.00"), savings.getBalance());
        assertEquals(new BigDecimal("300.00"), savings.availableToSpend());
        assertEquals(AccountType.SAVINGS, savings.getType());
    }

    @Test
    void debit_failsWhenExceedingAvailableFunds() {
        SavingsAccount savings = new SavingsAccount(
                10L, "SAV-000001", new BigDecimal("1000.00"),
                new NoFeeStrategy(), AccountStatus.ACTIVE, new BigDecimal("2.50")
        );
        savings.credit(new BigDecimal("100.00"));

        assertThrows(IllegalStateException.class, () -> savings.debit(new BigDecimal("150.00")));
    }

    @Test
    void statusRules_forSendingAndReceiving() {
        CheckingAccount account = new CheckingAccount(
                10L, "CHK-000001", new BigDecimal("5000.00"),
                new NoFeeStrategy(), AccountStatus.ACTIVE, BigDecimal.ZERO
        );
        account.credit(new BigDecimal("50.00"));

        account.freeze();
        assertTrue(account.canReceive());
        assertFalse(account.canSend());
        assertThrows(IllegalStateException.class, () -> account.debit(new BigDecimal("10.00")));
        account.credit(new BigDecimal("10.00")); // Receiving allowed when frozen

        account.unfreeze();
        assertTrue(account.canSend());
        assertTrue(account.canReceive());

        // Close account requires zero balance
        assertThrows(IllegalStateException.class, account::close);
        account.debit(new BigDecimal("60.00"));
        account.close();
        assertFalse(account.canSend());
        assertFalse(account.canReceive());
        assertThrows(IllegalStateException.class, () -> account.credit(new BigDecimal("10.00")));
    }

    @Test
    void accessManagement_ownerAndAuthorizedCustomers() {
        CheckingAccount account = new CheckingAccount(
                10L, "CHK-000001", new BigDecimal("5000.00"),
                new NoFeeStrategy(), AccountStatus.ACTIVE, BigDecimal.ZERO
        );

        assertTrue(account.hasAccess(10L));
        assertFalse(account.hasAccess(20L));

        account.grantAccess(20L);
        assertTrue(account.hasAccess(20L));

        account.revokeAccess(20L);
        assertFalse(account.hasAccess(20L));
    }

    @Test
    void changeFeeStrategy_updatesStrategy() {
        CheckingAccount account = new CheckingAccount(
                10L, "CHK-000001", new BigDecimal("5000.00"),
                new FixedFeeStrategy(new BigDecimal("2.00")),
                AccountStatus.ACTIVE, BigDecimal.ZERO
        );

        assertEquals(new BigDecimal("2.00"), account.calculateFee(new BigDecimal("100.00")));
        account.changeFeeStrategy(new NoFeeStrategy());
        assertEquals(new BigDecimal("0.00"), account.calculateFee(new BigDecimal("100.00")));
    }
}
