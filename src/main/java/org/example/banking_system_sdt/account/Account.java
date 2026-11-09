package org.example.banking_system_sdt.account;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.example.banking_system_sdt.exception.BadRequestException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "accounts")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "account_type")
public abstract class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false)
    private Long ownerId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "account_authorized_customers", joinColumns = @JoinColumn(name = "account_id"))
    @Column(name = "customer_id")
    private Set<Long> authorizedCustomerIds = new HashSet<>();

    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(nullable = false)
    private String currency = "EUR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(nullable = false)
    private BigDecimal dailyTransferLimit;

    @Convert(converter = FeeStrategyConverter.class)
    @Column(name = "fee_strategy", nullable = false)
    private FeeStrategy feeStrategy;

    @Version
    private Long version;

    protected Account() {
    }

    protected Account(Long ownerId, String accountNumber, BigDecimal dailyTransferLimit, FeeStrategy feeStrategy, AccountStatus status) {
        this.ownerId = ownerId;
        this.accountNumber = accountNumber;
        this.dailyTransferLimit = dailyTransferLimit;
        this.feeStrategy = feeStrategy;
        this.status = status != null ? status : AccountStatus.ACTIVE;
        this.balance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.currency = "EUR";
        this.authorizedCustomerIds = new HashSet<>();
    }

    public abstract BigDecimal availableToSpend();

    public abstract AccountType getType();

    public boolean canSend() {
        return status == AccountStatus.ACTIVE;
    }

    public boolean canReceive() {
        return status == AccountStatus.ACTIVE || status == AccountStatus.FROZEN;
    }

    public boolean hasAccess(Long userId) {
        if (userId == null) {
            return false;
        }
        return (ownerId != null && ownerId.equals(userId))
                || (authorizedCustomerIds != null && authorizedCustomerIds.contains(userId));
    }

    public BigDecimal calculateFee(BigDecimal amount) {
        if (feeStrategy == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return feeStrategy.calculateFee(amount);
    }

    public void debit(BigDecimal amount) {
        if (!canSend()) {
            throw new IllegalStateException("Account cannot send funds (status: " + status + ")");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        if (availableToSpend().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds in account: " + accountNumber);
        }
        this.balance = this.balance.subtract(amount).setScale(2, RoundingMode.HALF_UP);
    }

    public void credit(BigDecimal amount) {
        if (!canReceive()) {
            throw new IllegalStateException("Account cannot receive funds (status: " + status + ")");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        this.balance = this.balance.add(amount).setScale(2, RoundingMode.HALF_UP);
    }

    public void grantAccess(Long customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }
        this.authorizedCustomerIds.add(customerId);
    }

    public void revokeAccess(Long customerId) {
        this.authorizedCustomerIds.remove(customerId);
    }

    public void freeze() {
        this.status = AccountStatus.FROZEN;
    }

    public void unfreeze() {
        this.status = AccountStatus.ACTIVE;
    }

    public void close() {
        if (balance.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("An account can be closed only with a zero balance");
        }
        this.status = AccountStatus.CLOSED;
    }

    public void changeFeeStrategy(FeeStrategy feeStrategy) {
        if (feeStrategy == null) {
            throw new IllegalArgumentException("FeeStrategy cannot be null");
        }
        this.feeStrategy = feeStrategy;
    }

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public Set<Long> getAuthorizedCustomerIds() {
        return authorizedCustomerIds;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public BigDecimal getDailyTransferLimit() {
        return dailyTransferLimit;
    }

    public FeeStrategy getFeeStrategy() {
        return feeStrategy;
    }

    public Long getVersion() {
        return version;
    }
}
