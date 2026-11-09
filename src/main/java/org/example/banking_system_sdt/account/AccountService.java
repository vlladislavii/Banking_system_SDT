package org.example.banking_system_sdt.account;

import org.example.banking_system_sdt.customer.CustomerService;
import org.example.banking_system_sdt.event.BankingEvent;
import org.example.banking_system_sdt.event.BankingEventPublisher;
import org.example.banking_system_sdt.event.EventType;
import org.example.banking_system_sdt.exception.BadRequestException;
import org.example.banking_system_sdt.exception.ForbiddenException;
import org.example.banking_system_sdt.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class AccountService {

    public static final String BANK_FEE_ACCOUNT_NUMBER = "CHK-SYSTEM-FEE";

    private final Map<AccountType, AccountFactory> factories;
    private final CustomerService customerService;
    private final BankingEventPublisher publisher;
    private final AccountRepository accountRepository;

    @Autowired
    public AccountService(List<AccountFactory> factoryList, CustomerService customerService,
                          BankingEventPublisher publisher, AccountRepository accountRepository) {
        this(factoryList.stream().collect(Collectors.toMap(AccountFactory::supportedType, Function.identity())),
                customerService, publisher, accountRepository);
    }

    public AccountService(Map<AccountType, AccountFactory> factories, CustomerService customerService,
                          BankingEventPublisher publisher, AccountRepository accountRepository) {
        this.factories = factories;
        this.customerService = customerService;
        this.publisher = publisher;
        this.accountRepository = accountRepository;
    }

    public Account openAccount(Long callerId, Long ownerId, AccountType type, BigDecimal initialDeposit) {
        customerService.getProfile(callerId, ownerId);

        AccountFactory factory = factories.get(type);
        if (factory == null) {
            throw new BadRequestException("Unsupported account type: " + type);
        }

        Account account = factory.openAccount(ownerId, initialDeposit);
        Account saved = accountRepository.save(account);

        if (publisher != null) {
            publisher.publish(new BankingEvent(
                    UUID.randomUUID(),
                    EventType.ACCOUNT_OPENED,
                    LocalDateTime.now(),
                    List.of(saved.getOwnerId()),
                    saved.getId(),
                    null,
                    "Account opened: " + saved.getAccountNumber() + " (" + saved.getType() + ")"
            ));
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public Account getAccount(Long callerId, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found with id: " + accountId));

        if (!account.hasAccess(callerId) && !customerService.isAdmin(callerId)) {
            throw new ForbiddenException("Caller does not have access to account: " + accountId);
        }
        return account;
    }

    @Transactional(readOnly = true)
    public Optional<Account> findAccount(Long accountId) {
        return accountRepository.findById(accountId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long callerId, Long accountId) {
        return getAccount(callerId, accountId).getBalance();
    }

    @Transactional(readOnly = true)
    public List<Account> listCustomerAccounts(Long callerId, Long customerId) {
        if (!customerService.isAdmin(callerId) && (callerId == null || !callerId.equals(customerId))) {
            throw new ForbiddenException("Caller cannot list accounts for customer: " + customerId);
        }
        return accountRepository.findByOwnerId(customerId);
    }

    public void grantAccess(Long callerId, Long accountId, Long customerId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found with id: " + accountId));
        if (!account.getOwnerId().equals(callerId) && !customerService.isAdmin(callerId)) {
            throw new ForbiddenException("Only owner or admin can grant access");
        }
        account.grantAccess(customerId);
        Account saved = accountRepository.save(account);

        if (publisher != null) {
            publisher.publish(new BankingEvent(
                    UUID.randomUUID(),
                    EventType.ACCESS_GRANTED,
                    LocalDateTime.now(),
                    List.of(saved.getOwnerId(), customerId),
                    saved.getId(),
                    null,
                    "Access granted to customer " + customerId + " for account: " + saved.getAccountNumber()
            ));
        }
    }

    public void revokeAccess(Long callerId, Long accountId, Long customerId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found with id: " + accountId));
        if (!account.getOwnerId().equals(callerId) && !customerService.isAdmin(callerId)) {
            throw new ForbiddenException("Only owner or admin can revoke access");
        }
        account.revokeAccess(customerId);
        Account saved = accountRepository.save(account);

        if (publisher != null) {
            publisher.publish(new BankingEvent(
                    UUID.randomUUID(),
                    EventType.ACCESS_REVOKED,
                    LocalDateTime.now(),
                    List.of(saved.getOwnerId(), customerId),
                    saved.getId(),
                    null,
                    "Access revoked from customer " + customerId + " for account: " + saved.getAccountNumber()
            ));
        }
    }

    public void freeze(Long callerId, Long accountId) {
        if (!customerService.isAdmin(callerId)) {
            throw new ForbiddenException("Only administrator can freeze an account");
        }
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found with id: " + accountId));
        account.freeze();
        Account saved = accountRepository.save(account);

        if (publisher != null) {
            publisher.publish(new BankingEvent(
                    UUID.randomUUID(),
                    EventType.ACCOUNT_STATUS_CHANGED,
                    LocalDateTime.now(),
                    List.of(saved.getOwnerId()),
                    saved.getId(),
                    null,
                    "Account status changed to " + saved.getStatus() + " for account: " + saved.getAccountNumber()
            ));
        }
    }

    public void unfreeze(Long callerId, Long accountId) {
        if (!customerService.isAdmin(callerId)) {
            throw new ForbiddenException("Only administrator can unfreeze an account");
        }
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found with id: " + accountId));
        account.unfreeze();
        Account saved = accountRepository.save(account);

        if (publisher != null) {
            publisher.publish(new BankingEvent(
                    UUID.randomUUID(),
                    EventType.ACCOUNT_STATUS_CHANGED,
                    LocalDateTime.now(),
                    List.of(saved.getOwnerId()),
                    saved.getId(),
                    null,
                    "Account status changed to " + saved.getStatus() + " for account: " + saved.getAccountNumber()
            ));
        }
    }

    public void close(Long callerId, Long accountId) {
        if (!customerService.isAdmin(callerId)) {
            throw new ForbiddenException("Only administrator can close an account");
        }
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found with id: " + accountId));
        account.close();
        Account saved = accountRepository.save(account);

        if (publisher != null) {
            publisher.publish(new BankingEvent(
                    UUID.randomUUID(),
                    EventType.ACCOUNT_STATUS_CHANGED,
                    LocalDateTime.now(),
                    List.of(saved.getOwnerId()),
                    saved.getId(),
                    null,
                    "Account status changed to " + saved.getStatus() + " for account: " + saved.getAccountNumber()
            ));
        }
    }

    public void waiveFees(Long callerId, Long accountId) {
        if (!customerService.isAdmin(callerId)) {
            throw new ForbiddenException("Only administrator can waive fees");
        }
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found with id: " + accountId));
        account.changeFeeStrategy(new NoFeeStrategy());
        Account saved = accountRepository.save(account);

        if (publisher != null) {
            publisher.publish(new BankingEvent(
                    UUID.randomUUID(),
                    EventType.FEE_POLICY_CHANGED,
                    LocalDateTime.now(),
                    List.of(saved.getOwnerId()),
                    saved.getId(),
                    null,
                    "Fee policy changed to NoFeeStrategy for account: " + saved.getAccountNumber()
            ));
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal quoteFee(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found with id: " + accountId));
        return account.calculateFee(amount);
    }

    public void applyTransfer(Long sourceId, Long targetId, BigDecimal amount, BigDecimal fee) {
        if (sourceId.equals(targetId)) {
            throw new BadRequestException("Source and target accounts must be different");
        }
        Account source = accountRepository.findById(sourceId)
                .orElseThrow(() -> new NotFoundException("Source account not found with id: " + sourceId));
        Account target = accountRepository.findById(targetId)
                .orElseThrow(() -> new NotFoundException("Target account not found with id: " + targetId));

        BigDecimal totalFee = fee != null ? fee : BigDecimal.ZERO;
        BigDecimal totalDebit = amount.add(totalFee);

        source.debit(totalDebit);
        target.credit(amount);

        accountRepository.save(source);
        accountRepository.save(target);

        if (totalFee.compareTo(BigDecimal.ZERO) > 0) {
            Account bankFeeAccount = accountRepository.findByAccountNumber(BANK_FEE_ACCOUNT_NUMBER)
                    .orElseThrow(() -> new IllegalStateException("Bank fee account not found"));
            bankFeeAccount.credit(totalFee);
            accountRepository.save(bankFeeAccount);
        }
    }
}
