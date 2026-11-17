package org.example.banking_system_sdt.notification;

import org.example.banking_system_sdt.event.BankingEvent;
import org.example.banking_system_sdt.event.EventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomerNotificationObserverTest {

    @Mock
    private NotificationService notificationService;

    @Test
    void onEvent_createsNotificationForEachRecipient() {
        CustomerNotificationObserver observer = new CustomerNotificationObserver(notificationService);

        BankingEvent event = new BankingEvent(
                UUID.randomUUID(),
                EventType.TRANSFER_COMPLETED,
                LocalDateTime.now(),
                List.of(10L, 20L),
                null,
                100L,
                "Transfer completed"
        );

        observer.onEvent(event);

        verify(notificationService).create(10L, EventType.TRANSFER_COMPLETED, "Transfer completed");
        verify(notificationService).create(20L, EventType.TRANSFER_COMPLETED, "Transfer completed");
    }
}
