package org.example.banking_system_sdt;

import org.example.banking_system_sdt.account.AccountRepository;
import org.example.banking_system_sdt.account.AccountService;
import org.example.banking_system_sdt.account.AccountStatus;
import org.example.banking_system_sdt.account.CheckingAccount;
import org.example.banking_system_sdt.account.NoFeeStrategy;
import org.example.banking_system_sdt.customer.Administrator;
import org.example.banking_system_sdt.customer.User;
import org.example.banking_system_sdt.customer.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public DataInitializer(UserRepository userRepository, AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    public void run(String... args) {
        User admin = userRepository.findByEmail("admin@bank.com").orElseGet(() -> {
            Administrator newAdmin = new Administrator("System Admin", "admin@bank.com", LocalDateTime.now());
            return userRepository.save(newAdmin);
        });

        if (accountRepository.findByAccountNumber(AccountService.BANK_FEE_ACCOUNT_NUMBER).isEmpty()) {
            CheckingAccount bankFeeAccount = new CheckingAccount(
                    admin.getId(),
                    AccountService.BANK_FEE_ACCOUNT_NUMBER,
                    new BigDecimal("1000000.00"),
                    new NoFeeStrategy(),
                    AccountStatus.ACTIVE,
                    BigDecimal.ZERO
            );
            accountRepository.save(bankFeeAccount);
        }
    }
}
