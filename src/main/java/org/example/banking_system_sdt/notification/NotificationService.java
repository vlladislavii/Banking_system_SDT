package org.example.banking_system_sdt.notification;

import org.example.banking_system_sdt.customer.CustomerService;
import org.example.banking_system_sdt.event.EventType;
import org.example.banking_system_sdt.exception.ForbiddenException;
import org.example.banking_system_sdt.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CustomerService customerService;

    public NotificationService(NotificationRepository notificationRepository, CustomerService customerService) {
        this.notificationRepository = notificationRepository;
        this.customerService = customerService;
    }

    public Notification create(Long recipientId, EventType type, String message) {
        Notification notification = new Notification(recipientId, type, message, LocalDateTime.now(), false);
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> listForCustomer(Long callerId, Long customerId) {
        customerService.getProfile(callerId, customerId);
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(customerId);
    }

    public void markRead(Long callerId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found with id: " + notificationId));

        if (!notification.getRecipientId().equals(callerId) && !customerService.isAdmin(callerId)) {
            throw new ForbiddenException("Caller cannot mark another user's notification as read");
        }

        notification.markRead();
        notificationRepository.save(notification);
    }
}
