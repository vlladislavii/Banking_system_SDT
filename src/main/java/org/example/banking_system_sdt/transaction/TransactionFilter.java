package org.example.banking_system_sdt.transaction;

import java.time.LocalDate;

public record TransactionFilter(
        TransactionStatus status,
        Long accountId,
        LocalDate from,
        LocalDate to
) {}
