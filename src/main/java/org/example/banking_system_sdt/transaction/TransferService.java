package org.example.banking_system_sdt.transaction;

import org.example.banking_system_sdt.event.BankingEvent;
import org.example.banking_system_sdt.event.BankingEventPublisher;
import org.example.banking_system_sdt.event.EventType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransferService {

    private final TransferValidationHandler validationChain;
    private final AccountClient accountClient;
    private final BankingEventPublisher publisher;
    private final TransactionRepository transactionRepository;

    public TransferService(TransferValidationHandler validationChain, AccountClient accountClient,
                           BankingEventPublisher publisher, TransactionRepository transactionRepository) {
        this.validationChain = validationChain;
        this.accountClient = accountClient;
        this.publisher = publisher;
        this.transactionRepository = transactionRepository;
    }

    public Transaction transfer(TransferRequest request) {
        Transaction tx = new Transaction(
                request.sourceAccountId(),
                request.targetAccountId(),
                request.callerId(),
                request.amount(),
                BigDecimal.ZERO,
                TransactionStatus.PENDING,
                null,
                LocalDateTime.now(),
                null
        );
        tx = transactionRepository.save(tx);

        Optional<AccountSnapshot> sourceOpt = Optional.ofNullable(request.sourceAccountId())
                .flatMap(accountClient::getAccount);
        Optional<AccountSnapshot> targetOpt = Optional.ofNullable(request.targetAccountId())
                .flatMap(accountClient::getAccount);

        BigDecimal fee = BigDecimal.ZERO;
        if (sourceOpt.isPresent() && request.amount() != null && request.amount().compareTo(BigDecimal.ZERO) > 0) {
            fee = accountClient.quoteFee(request.sourceAccountId(), request.amount());
        }

        BigDecimal sentToday = BigDecimal.ZERO;
        if (request.sourceAccountId() != null) {
            sentToday = transactionRepository.calculateSentToday(
                    request.sourceAccountId(),
                    LocalDate.now().atStartOfDay()
            );
        }

        TransferContext context = new TransferContext(
                request,
                sourceOpt.orElse(null),
                targetOpt.orElse(null),
                fee,
                sentToday
        );

        ValidationResult result = validationChain.validate(context);

        if (result.valid()) {
            try {
                accountClient.applyTransfer(
                        request.sourceAccountId(),
                        request.targetAccountId(),
                        request.amount(),
                        fee
                );
                tx.markSuccessful(fee);
                tx = transactionRepository.save(tx);

                List<Long> recipients = new ArrayList<>();
                if (context.getSource() != null) {
                    recipients.add(context.getSource().ownerId());
                }
                if (context.getTarget() != null
                        && (context.getSource() == null || !context.getTarget().ownerId().equals(context.getSource().ownerId()))) {
                    recipients.add(context.getTarget().ownerId());
                }

                if (publisher != null) {
                    publisher.publish(new BankingEvent(
                            UUID.randomUUID(),
                            EventType.TRANSFER_COMPLETED,
                            LocalDateTime.now(),
                            recipients,
                            request.sourceAccountId(),
                            tx.getId(),
                            String.format("Transfer of %s EUR completed from account %d to account %d (fee: %s EUR)",
                                    request.amount(), request.sourceAccountId(), request.targetAccountId(), fee)
                    ));
                }
            } catch (ObjectOptimisticLockingFailureException e) {
                tx.markRejected("Concurrent modification: account was modified by another operation. Please retry.");
                tx = transactionRepository.save(tx);
                publishRejectedEvent(tx, request, tx.getRejectionReason());
            } catch (Exception e) {
                tx.markRejected(e.getMessage());
                tx = transactionRepository.save(tx);
                publishRejectedEvent(tx, request, e.getMessage());
            }
        } else {
            tx.markRejected(result.reason());
            tx = transactionRepository.save(tx);
            publishRejectedEvent(tx, request, result.reason());
        }

        return tx;
    }

    private void publishRejectedEvent(Transaction tx, TransferRequest request, String reason) {
        if (publisher != null) {
            List<Long> recipients = request.callerId() != null ? List.of(request.callerId()) : List.of();
            publisher.publish(new BankingEvent(
                    UUID.randomUUID(),
                    EventType.TRANSFER_REJECTED,
                    LocalDateTime.now(),
                    recipients,
                    request.sourceAccountId(),
                    tx.getId(),
                    String.format("Transfer of %s EUR rejected: %s", request.amount(), reason)
            ));
        }
    }
}
