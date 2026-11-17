package org.example.banking_system_sdt.transaction;

import org.example.banking_system_sdt.event.BankingEvent;
import org.example.banking_system_sdt.event.BankingEventPublisher;
import org.example.banking_system_sdt.event.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private TransferValidationHandler validationChain;

    @Mock
    private AccountClient accountClient;

    @Mock
    private BankingEventPublisher publisher;

    @Mock
    private TransactionRepository transactionRepository;

    private TransferService transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferService(validationChain, accountClient, publisher, transactionRepository);
    }

    private AccountSnapshot createSource() {
        return new AccountSnapshot(1L, 10L, Set.of(), true, true, new BigDecimal("1000.00"), new BigDecimal("5000.00"));
    }

    private AccountSnapshot createTarget() {
        return new AccountSnapshot(2L, 20L, Set.of(), true, true, new BigDecimal("500.00"), new BigDecimal("5000.00"));
    }

    @Test
    void transfer_successful_marksSuccessfulAndPublishesEvent() {
        TransferRequest request = new TransferRequest(10L, 1L, 2L, new BigDecimal("100.00"));

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountClient.getAccount(1L)).thenReturn(Optional.of(createSource()));
        when(accountClient.getAccount(2L)).thenReturn(Optional.of(createTarget()));
        when(accountClient.quoteFee(1L, new BigDecimal("100.00"))).thenReturn(new BigDecimal("1.00"));
        when(transactionRepository.calculateSentToday(eq(1L), any(LocalDateTime.class))).thenReturn(BigDecimal.ZERO);
        when(validationChain.validate(any(TransferContext.class))).thenReturn(ValidationResult.ok());

        Transaction tx = transferService.transfer(request);

        assertNotNull(tx);
        assertEquals(TransactionStatus.SUCCESSFUL, tx.getStatus());
        assertEquals(new BigDecimal("1.00"), tx.getFee());
        assertNotNull(tx.getCompletedAt());
        assertNull(tx.getRejectionReason());

        verify(accountClient).applyTransfer(1L, 2L, new BigDecimal("100.00"), new BigDecimal("1.00"));

        ArgumentCaptor<BankingEvent> eventCaptor = ArgumentCaptor.forClass(BankingEvent.class);
        verify(publisher).publish(eventCaptor.capture());
        assertEquals(EventType.TRANSFER_COMPLETED, eventCaptor.getValue().getType());
        assertTrue(eventCaptor.getValue().getRecipientIds().contains(10L));
        assertTrue(eventCaptor.getValue().getRecipientIds().contains(20L));
    }

    @Test
    void transfer_validationFails_marksRejectedAndPublishesEvent() {
        TransferRequest request = new TransferRequest(10L, 1L, 2L, new BigDecimal("2000.00"));

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountClient.getAccount(1L)).thenReturn(Optional.of(createSource()));
        when(accountClient.getAccount(2L)).thenReturn(Optional.of(createTarget()));
        when(accountClient.quoteFee(1L, new BigDecimal("2000.00"))).thenReturn(new BigDecimal("1.00"));
        when(transactionRepository.calculateSentToday(eq(1L), any(LocalDateTime.class))).thenReturn(BigDecimal.ZERO);
        when(validationChain.validate(any(TransferContext.class)))
                .thenReturn(ValidationResult.rejected("Insufficient funds"));

        Transaction tx = transferService.transfer(request);

        assertNotNull(tx);
        assertEquals(TransactionStatus.REJECTED, tx.getStatus());
        assertEquals("Insufficient funds", tx.getRejectionReason());

        verify(accountClient, never()).applyTransfer(any(), any(), any(), any());

        ArgumentCaptor<BankingEvent> eventCaptor = ArgumentCaptor.forClass(BankingEvent.class);
        verify(publisher).publish(eventCaptor.capture());
        assertEquals(EventType.TRANSFER_REJECTED, eventCaptor.getValue().getType());
        assertTrue(eventCaptor.getValue().getRecipientIds().contains(10L));
    }

    @Test
    void transfer_optimisticLockException_marksRejectedWithRetry() {
        TransferRequest request = new TransferRequest(10L, 1L, 2L, new BigDecimal("100.00"));

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountClient.getAccount(1L)).thenReturn(Optional.of(createSource()));
        when(accountClient.getAccount(2L)).thenReturn(Optional.of(createTarget()));
        when(accountClient.quoteFee(1L, new BigDecimal("100.00"))).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.calculateSentToday(eq(1L), any(LocalDateTime.class))).thenReturn(BigDecimal.ZERO);
        when(validationChain.validate(any(TransferContext.class))).thenReturn(ValidationResult.ok());

        doThrow(new ObjectOptimisticLockingFailureException("Account", 1L))
                .when(accountClient).applyTransfer(1L, 2L, new BigDecimal("100.00"), BigDecimal.ZERO);

        Transaction tx = transferService.transfer(request);

        assertEquals(TransactionStatus.REJECTED, tx.getStatus());
        assertTrue(tx.getRejectionReason().contains("Concurrent modification"));

        ArgumentCaptor<BankingEvent> eventCaptor = ArgumentCaptor.forClass(BankingEvent.class);
        verify(publisher).publish(eventCaptor.capture());
        assertEquals(EventType.TRANSFER_REJECTED, eventCaptor.getValue().getType());
    }
}
