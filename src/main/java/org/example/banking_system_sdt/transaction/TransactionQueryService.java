package org.example.banking_system_sdt.transaction;

import org.example.banking_system_sdt.exception.ForbiddenException;
import org.example.banking_system_sdt.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TransactionQueryService {

    private final AccountClient accountClient;
    private final TransactionRepository transactionRepository;

    public TransactionQueryService(AccountClient accountClient, TransactionRepository transactionRepository) {
        this.accountClient = accountClient;
        this.transactionRepository = transactionRepository;
    }

    public List<Transaction> getAccountHistory(Long callerId, Long accountId) {
        AccountSnapshot account = accountClient.getAccount(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found with id: " + accountId));

        if (!account.hasAccess(callerId) && !accountClient.isAdmin(callerId)) {
            throw new ForbiddenException("Caller does not have access to account history for account: " + accountId);
        }

        return transactionRepository.findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(accountId, accountId);
    }

    public List<Transaction> search(Long callerId, TransactionFilter filter) {
        if (!accountClient.isAdmin(callerId)) {
            throw new ForbiddenException("Only administrators can search transactions");
        }

        return transactionRepository.findAll().stream()
                .filter(tx -> filter == null || filter.status() == null || tx.getStatus() == filter.status())
                .filter(tx -> filter == null || filter.accountId() == null
                        || tx.getSourceAccountId().equals(filter.accountId())
                        || tx.getTargetAccountId().equals(filter.accountId()))
                .filter(tx -> filter == null || filter.from() == null
                        || !tx.getCreatedAt().toLocalDate().isBefore(filter.from()))
                .filter(tx -> filter == null || filter.to() == null
                        || !tx.getCreatedAt().toLocalDate().isAfter(filter.to()))
                .toList();
    }
}
