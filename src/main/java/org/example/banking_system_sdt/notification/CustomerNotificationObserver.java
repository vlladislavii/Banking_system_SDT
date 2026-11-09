package org.example.banking_system_sdt.notification;

import org.example.banking_system_sdt.event.BankingEvent;
import org.example.banking_system_sdt.event.BankingEventObserver;
import org.springframework.stereotype.Component;

@Component
public class CustomerNotificationObserver implements BankingEventObserver {

    private final NotificationService notificationService;

    public CustomerNotificationObserver(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void onEvent(BankingEvent event) {
        if (event == null || event.getRecipientIds() == null) {
            return;
        }
        for (Long recipientId : event.getRecipientIds()) {
            notificationService.create(recipientId, event.getType(), event.getMessage());
        }
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }
}
