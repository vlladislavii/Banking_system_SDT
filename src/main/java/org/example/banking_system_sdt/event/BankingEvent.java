package org.example.banking_system_sdt.event;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class BankingEvent {

    private final UUID id;
    private final EventType type;
    private final LocalDateTime occurredAt;
    private final List<Long> recipientIds;
    private final Long accountId;
    private final Long transactionId;
    private final String message;

    public BankingEvent(UUID id, EventType type, LocalDateTime occurredAt, List<Long> recipientIds,
                        Long accountId, Long transactionId, String message) {
        this.id = id != null ? id : UUID.randomUUID();
        this.type = type;
        this.occurredAt = occurredAt != null ? occurredAt : LocalDateTime.now();
        this.recipientIds = recipientIds != null ? Collections.unmodifiableList(new ArrayList<>(recipientIds)) : Collections.emptyList();
        this.accountId = accountId;
        this.transactionId = transactionId;
        this.message = message;
    }

    public UUID getId() {
        return id;
    }

    public EventType getType() {
        return type;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public List<Long> getRecipientIds() {
        return recipientIds;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BankingEvent that = (BankingEvent) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BankingEvent{" +
                "id=" + id +
                ", type=" + type +
                ", occurredAt=" + occurredAt +
                ", recipientIds=" + recipientIds +
                ", accountId=" + accountId +
                ", transactionId=" + transactionId +
                ", message='" + message + '\'' +
                '}';
    }
}
