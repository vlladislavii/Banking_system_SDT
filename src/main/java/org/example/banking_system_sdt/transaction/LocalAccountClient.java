package org.example.banking_system_sdt.transaction;

import org.example.banking_system_sdt.account.AccountService;
import org.example.banking_system_sdt.customer.CustomerService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class LocalAccountClient implements AccountClient {

    private final AccountService accountService;
    private final CustomerService customerService;

    public LocalAccountClient(AccountService accountService, CustomerService customerService) {
        this.accountService = accountService;
        this.customerService = customerService;
    }

    @Override
    public Optional<AccountSnapshot> getAccount(Long accountId) {
        return accountService.findAccount(accountId).map(acc -> new AccountSnapshot(
                acc.getId(),
                acc.getOwnerId(),
                acc.getAuthorizedCustomerIds(),
                acc.canSend(),
                acc.canReceive(),
                acc.availableToSpend(),
                acc.getDailyTransferLimit()
        ));
    }

    @Override
    public BigDecimal quoteFee(Long accountId, BigDecimal amount) {
        return accountService.quoteFee(accountId, amount);
    }

    @Override
    public void applyTransfer(Long sourceId, Long targetId, BigDecimal amount, BigDecimal fee) {
        accountService.applyTransfer(sourceId, targetId, amount, fee);
    }

    @Override
    public boolean isAdmin(Long userId) {
        return customerService.isAdmin(userId);
    }
}
