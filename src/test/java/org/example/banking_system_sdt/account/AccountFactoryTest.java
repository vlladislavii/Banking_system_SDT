package org.example.banking_system_sdt.account;

import org.example.banking_system_sdt.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AccountFactoryTest {

    @Test
    void checkingAccountFactory_createsConfiguredCheckingAccount() {
        CheckingAccountFactory factory = new CheckingAccountFactory();
        assertEquals(AccountType.CHECKING, factory.supportedType());

        Account account = factory.openAccount(100L, new BigDecimal("50.00"));

        assertInstanceOf(CheckingAccount.class, account);
        assertEquals(100L, account.getOwnerId());
        assertTrue(account.getAccountNumber().startsWith("CHK-"));
        assertEquals(new BigDecimal("50.00"), account.getBalance());
        assertEquals(new BigDecimal("550.00"), account.availableToSpend());
        assertEquals(new BigDecimal("5000.00"), account.getDailyTransferLimit());
        assertEquals(AccountStatus.ACTIVE, account.getStatus());
        assertEquals(new BigDecimal("1.00"), account.calculateFee(new BigDecimal("100.00")));
    }

    @Test
    void savingsAccountFactory_createsConfiguredSavingsAccount() {
        SavingsAccountFactory factory = new SavingsAccountFactory();
        assertEquals(AccountType.SAVINGS, factory.supportedType());

        Account account = factory.openAccount(200L, new BigDecimal("150.00"));

        assertInstanceOf(SavingsAccount.class, account);
        assertEquals(200L, account.getOwnerId());
        assertTrue(account.getAccountNumber().startsWith("SAV-"));
        assertEquals(new BigDecimal("150.00"), account.getBalance());
        assertEquals(new BigDecimal("150.00"), account.availableToSpend());
        assertEquals(new BigDecimal("1000.00"), account.getDailyTransferLimit());
        assertEquals(AccountStatus.ACTIVE, account.getStatus());
        // 200 * 0.5% = 1.00 fee
        assertEquals(new BigDecimal("1.00"), account.calculateFee(new BigDecimal("200.00")));
    }

    @Test
    void savingsAccountFactory_rejectsDepositBelowMinimum() {
        SavingsAccountFactory factory = new SavingsAccountFactory();

        assertThrows(BadRequestException.class, () -> factory.openAccount(200L, new BigDecimal("50.00")));
        assertThrows(BadRequestException.class, () -> factory.openAccount(200L, BigDecimal.ZERO));
    }
}
