package org.example.banking_system_sdt.notification;

import org.example.banking_system_sdt.customer.Customer;
import org.example.banking_system_sdt.customer.CustomerService;
import org.example.banking_system_sdt.event.EventType;
import org.example.banking_system_sdt.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private CustomerService customerService;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, customerService);
    }

    @Test
    void create_savesNotification() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        Notification notification = notificationService.create(10L, EventType.ACCOUNT_OPENED, "Welcome");

        assertNotNull(notification);
        assertEquals(10L, notification.getRecipientId());
        assertEquals(EventType.ACCOUNT_OPENED, notification.getType());
        assertEquals("Welcome", notification.getMessage());
        assertFalse(notification.isRead());
    }

    @Test
    void listForCustomer_validatesAccess() {
        when(customerService.getProfile(10L, 10L)).thenReturn(new Customer("Test", "t@t.com", "1", "A", LocalDateTime.now()));
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(
                new Notification(10L, EventType.ACCOUNT_OPENED, "Welcome", LocalDateTime.now(), false)
        ));

        List<Notification> list = notificationService.listForCustomer(10L, 10L);
        assertEquals(1, list.size());
    }

    @Test
    void markRead_allowsRecipient() {
        Notification notification = new Notification(10L, EventType.ACCOUNT_OPENED, "Msg", LocalDateTime.now(), false);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.markRead(10L, 1L);

        assertTrue(notification.isRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markRead_forbiddenForOtherUser() {
        Notification notification = new Notification(10L, EventType.ACCOUNT_OPENED, "Msg", LocalDateTime.now(), false);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(customerService.isAdmin(20L)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> notificationService.markRead(20L, 1L));
        assertFalse(notification.isRead());
    }
}
