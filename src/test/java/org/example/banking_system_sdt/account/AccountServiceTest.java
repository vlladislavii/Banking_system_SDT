package org.example.banking_system_sdt.account;

import org.example.banking_system_sdt.customer.Customer;
import org.example.banking_system_sdt.customer.CustomerService;
import org.example.banking_system_sdt.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private CustomerService customerService;

    @Mock
    private AccountRepository accountRepository;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        List<AccountFactory> factories = List.of(new CheckingAccountFactory(), new SavingsAccountFactory());
        accountService = new AccountService(factories, customerService, accountRepository);
    }

    @Test
    void openAccount_validOwner_createsAndSaves() {
        Customer customer = new Customer("Alice", "alice@test.com", "111", "Addr", LocalDateTime.now());
        when(customerService.getProfile(10L, 10L)).thenReturn(customer);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account account = accountService.openAccount(10L, 10L, AccountType.CHECKING, new BigDecimal("100.00"));

        assertNotNull(account);
        assertEquals(10L, account.getOwnerId());
        assertEquals(AccountType.CHECKING, account.getType());
        assertEquals(new BigDecimal("100.00"), account.getBalance());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void getAccount_allowsOwner() {
        CheckingAccount account = new CheckingAccount(
                10L, "CHK-000001", new BigDecimal("5000.00"),
                new NoFeeStrategy(), AccountStatus.ACTIVE, BigDecimal.ZERO
        );
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        Account result = accountService.getAccount(10L, 1L);
        assertEquals("CHK-000001", result.getAccountNumber());
    }

    @Test
    void getAccount_allowsAdmin() {
        CheckingAccount account = new CheckingAccount(
                10L, "CHK-000001", new BigDecimal("5000.00"),
                new NoFeeStrategy(), AccountStatus.ACTIVE, BigDecimal.ZERO
        );
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(customerService.isAdmin(99L)).thenReturn(true);

        Account result = accountService.getAccount(99L, 1L);
        assertEquals("CHK-000001", result.getAccountNumber());
    }

    @Test
    void getAccount_forbiddenForUnauthorizedUser() {
        CheckingAccount account = new CheckingAccount(
                10L, "CHK-000001", new BigDecimal("5000.00"),
                new NoFeeStrategy(), AccountStatus.ACTIVE, BigDecimal.ZERO
        );
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(customerService.isAdmin(20L)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> accountService.getAccount(20L, 1L));
    }

    @Test
    void waiveFees_adminOnly_swapsToNoFeeStrategy() {
        CheckingAccount account = new CheckingAccount(
                10L, "CHK-000001", new BigDecimal("5000.00"),
                new FixedFeeStrategy(new BigDecimal("2.00")),
                AccountStatus.ACTIVE, BigDecimal.ZERO
        );
        when(customerService.isAdmin(1L)).thenReturn(true);
        when(accountRepository.findById(100L)).thenReturn(Optional.of(account));

        accountService.waiveFees(1L, 100L);

        assertInstanceOf(NoFeeStrategy.class, account.getFeeStrategy());
        verify(accountRepository).save(account);

        when(customerService.isAdmin(2L)).thenReturn(false);
        assertThrows(ForbiddenException.class, () -> accountService.waiveFees(2L, 100L));
    }

    @Test
    void applyTransfer_debitsSourceCreditsTargetAndCreditsBankFeeAccount() {
        CheckingAccount source = new CheckingAccount(
                10L, "CHK-000001", new BigDecimal("5000.00"),
                new FixedFeeStrategy(new BigDecimal("1.00")),
                AccountStatus.ACTIVE, BigDecimal.ZERO
        );
        source.credit(new BigDecimal("500.00"));

        SavingsAccount target = new SavingsAccount(
                20L, "SAV-000001", new BigDecimal("1000.00"),
                new PercentageFeeStrategy(new BigDecimal("0.5")),
                AccountStatus.ACTIVE, new BigDecimal("2.00")
        );
        target.credit(new BigDecimal("100.00"));

        CheckingAccount bankFeeAccount = new CheckingAccount(
                1L, AccountService.BANK_FEE_ACCOUNT_NUMBER, new BigDecimal("1000000.00"),
                new NoFeeStrategy(), AccountStatus.ACTIVE, BigDecimal.ZERO
        );

        when(accountRepository.findById(1L)).thenReturn(Optional.of(source));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(target));
        when(accountRepository.findByAccountNumber(AccountService.BANK_FEE_ACCOUNT_NUMBER))
                .thenReturn(Optional.of(bankFeeAccount));

        accountService.applyTransfer(1L, 2L, new BigDecimal("100.00"), new BigDecimal("1.00"));

        // Source debited: 100.00 + 1.00 = 101.00 (remaining: 399.00)
        assertEquals(new BigDecimal("399.00"), source.getBalance());
        // Target credited: 100.00 (new balance: 200.00)
        assertEquals(new BigDecimal("200.00"), target.getBalance());
        // Bank fee account credited: 1.00
        assertEquals(new BigDecimal("1.00"), bankFeeAccount.getBalance());

        verify(accountRepository).save(source);
        verify(accountRepository).save(target);
        verify(accountRepository).save(bankFeeAccount);
    }
}
