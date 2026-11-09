package org.example.banking_system_sdt.transaction.dto;

import org.example.banking_system_sdt.transaction.Transaction;
import org.example.banking_system_sdt.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        Long sourceAccountId,
        Long targetAccountId,
        Long initiatedBy,
        BigDecimal amount,
        BigDecimal fee,
        TransactionStatus status,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
    public static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getSourceAccountId(),
                tx.getTargetAccountId(),
                tx.getInitiatedBy(),
                tx.getAmount(),
                tx.getFee(),
                tx.getStatus(),
                tx.getRejectionReason(),
                tx.getCreatedAt(),
                tx.getCompletedAt()
        );
    }
}
