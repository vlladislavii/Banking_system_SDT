package org.example.banking_system_sdt.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sourceAccountId;

    @Column(nullable = false)
    private Long targetAccountId;

    @Column(nullable = false)
    private Long initiatedBy;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private BigDecimal fee = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(length = 1000)
    private String rejectionReason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime completedAt;

    public Transaction() {
    }

    public Transaction(Long sourceAccountId, Long targetAccountId, Long initiatedBy, BigDecimal amount,
                       BigDecimal fee, TransactionStatus status, String rejectionReason,
                       LocalDateTime createdAt, LocalDateTime completedAt) {
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.initiatedBy = initiatedBy;
        this.amount = amount;
        this.fee = fee != null ? fee : BigDecimal.ZERO;
        this.status = status != null ? status : TransactionStatus.PENDING;
        this.rejectionReason = rejectionReason;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.completedAt = completedAt;
    }

    public void markSuccessful(BigDecimal fee) {
        if (this.status != TransactionStatus.PENDING) {
            throw new IllegalStateException("Cannot mark transaction as successful from status: " + this.status);
        }
        this.status = TransactionStatus.SUCCESSFUL;
        this.fee = fee != null ? fee : BigDecimal.ZERO;
        this.completedAt = LocalDateTime.now();
    }

    public void markRejected(String reason) {
        if (this.status != TransactionStatus.PENDING) {
            throw new IllegalStateException("Cannot mark transaction as rejected from status: " + this.status);
        }
        this.status = TransactionStatus.REJECTED;
        this.rejectionReason = reason;
        this.completedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getSourceAccountId() {
        return sourceAccountId;
    }

    public Long getTargetAccountId() {
        return targetAccountId;
    }

    public Long getInitiatedBy() {
        return initiatedBy;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
